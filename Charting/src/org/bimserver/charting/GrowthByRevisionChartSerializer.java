package org.bimserver.charting;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang.reflect.FieldUtils;
import org.bimserver.BimServer;
import org.bimserver.charting.Charts.BumpChart;
import org.bimserver.client.DirectBimServerClientFactory;
import org.bimserver.database.BimserverDatabaseException;
import org.bimserver.database.DatabaseSession;
import org.bimserver.database.Query;
import org.bimserver.database.Query.Deep;
import org.bimserver.database.actions.AbstractDownloadDatabaseAction;
import org.bimserver.emf.IfcModelInterface;
import org.bimserver.emf.PackageMetaData;
import org.bimserver.ifc.IfcModel;
import org.bimserver.models.ifc2x3tc1.IfcObjectDefinition;
import org.bimserver.models.ifc2x3tc1.IfcPropertyDefinition;
import org.bimserver.models.ifc2x3tc1.IfcRelationship;
import org.bimserver.models.store.ConcreteRevision;
import org.bimserver.models.store.Project;
import org.bimserver.models.store.Revision;
import org.bimserver.models.store.StorePackage;
import org.bimserver.plugins.PluginManager;
import org.bimserver.plugins.renderengine.RenderEnginePlugin;
import org.bimserver.plugins.serializers.ProjectInfo;
import org.bimserver.plugins.serializers.SerializerException;
import org.bimserver.utils.UTF8PrintWriter;
import org.bimserver.webservices.PublicInterfaceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GrowthByRevisionChartSerializer extends ChartEmfSerializer {

	private static final Logger LOGGER = LoggerFactory.getLogger(GrowthByRevisionChartSerializer.class);

	public enum HorizontalDimension {
		Revision, DateOfRevision
	};

	public static Comparator<Entry<Object, IfcRootBreakdown>> sortLargerKeysToEnd = new Comparator<Entry<Object, IfcRootBreakdown>>() {
		public int compare(Entry<Object, IfcRootBreakdown> p1, Entry<Object, IfcRootBreakdown> p2) {
			Object key1 = p1.getKey();
			Object key2 = p2.getKey();
			//
			Long first = 0L;
			if (key1 instanceof Date)
				first = ((Date) key1).getTime();
			else
				first = (Long) key1;
			//
			Long second = 0L;
			if (key2 instanceof Date)
				second = ((Date) key2).getTime();
			else
				second = (Long) key2;
			//
			return first.compareTo(second);
		}
	};

	// Break IfcRoot into: IfcPropertyDefinition, IfcRelationship, and IfcObjectDefinition.
	private class IfcRootBreakdown {
		public long IfcPropertyDefinitionsCount = 0;
		public long IfcRelationshipsCount = 0;
		public long IfcObjectDefinitionsCount = 0;
		public long MiscellaneousCount = 0;

		public IfcRootBreakdown() {
		}

		public void integrateIfcModel(IfcModel subModel) {
			// Objects able to be properties.
			List<IfcPropertyDefinition> ifcPropertyDefinitions = subModel.getAllWithSubTypes(IfcPropertyDefinition.class);
			long thisIfcPropertyDefinitionsSize = ifcPropertyDefinitions.size();
			IfcPropertyDefinitionsCount += thisIfcPropertyDefinitionsSize;
			ifcPropertyDefinitions.clear();
			// Relationships.
			List<IfcRelationship> ifcRelationships = subModel.getAllWithSubTypes(IfcRelationship.class);
			long thisIfcRelationshipsSize = ifcRelationships.size();
			IfcRelationshipsCount += thisIfcRelationshipsSize;
			ifcRelationships.clear();
			// Object definitions.
			List<IfcObjectDefinition> ifcObjectDefinitions = subModel.getAllWithSubTypes(IfcObjectDefinition.class);
			long thisIfcObjectDefinitionsSize = ifcObjectDefinitions.size();
			IfcObjectDefinitionsCount += thisIfcObjectDefinitionsSize;
			ifcObjectDefinitions.clear();
			// Everything else.
			MiscellaneousCount += subModel.size() - (thisIfcPropertyDefinitionsSize + thisIfcRelationshipsSize + thisIfcObjectDefinitionsSize);
		}
	}

	@Override
	public void init(IfcModelInterface model, ProjectInfo projectInfo, PluginManager pluginManager, RenderEnginePlugin renderEnginePlugin, PackageMetaData packageMetaData, boolean normalizeOids) throws SerializerException {
		super.init(model, projectInfo, pluginManager, renderEnginePlugin, packageMetaData, normalizeOids);
		// Pick chart.
		chart = new BumpChart();
		chart.setDimensionLookupKey("group", "group");
		chart.setDimensionLookupKey("date", "date");
		chart.setDimensionLookupKey("size", "size");
		chart.FitToSize = false;
		integrateSettings();
		// Prepare for data.
		rawData = new ArrayList<>();
		// Get horizontal dimension request.
		Object possibleDimension = (hasOption("Horizontal Dimension")) ? getOptionValue("Horizontal Dimension") : HorizontalDimension.Revision;
		HorizontalDimension dimension = (possibleDimension != null) ? (HorizontalDimension) possibleDimension : HorizontalDimension.Revision;
		boolean isByRevision = (dimension == HorizontalDimension.Revision) ? true : false;
		//
		boolean useDelta = (hasOption("Only Calculate Changes")) ? (boolean) getOptionValue("Only Calculate Changes") : false;
		boolean startDeltaFromZero = (hasOption("Start Changes From Zero")) ? (boolean) getOptionValue("Start Changes From Zero") : false;
		// Pick the choice that's been asked for later: Integer or Date.
		LinkedHashMap<Object, IfcRootBreakdown> breakdownByChoice = new LinkedHashMap<>();
		// Obtain session.
		DatabaseSession session = getNewDatabaseSession(pluginManager);
		// Obtain any revision.
		long realRidForTesting = projectInfo.getRevisionId();
		Revision revision = getRevisionByRevisionId(session, realRidForTesting);
		if (revision == null) {
			//System.out.println("Revision with rid " + realRidForTesting + " not found");
		} else {
			Project project = revision.getProject();
			project.getGeoTag().load();
			// IfcModelInterface ifcModel = session.executeAndCommitAction(action);
			for (Revision thisRevision : project.getRevisions()) {
				// long incrSize = 0;
				Long revisionId = ((Number) thisRevision.getId()).longValue();
				for (ConcreteRevision subRevision : thisRevision.getConcreteRevisions()) {
					// incrSize += subRevision.getSize();
					int highestStopId = AbstractDownloadDatabaseAction.findHighestStopRid(project, subRevision);
					Query query = new Query(packageMetaData, subRevision.getProject().getId(), subRevision.getId(), null, Deep.YES, highestStopId);
					try {
						// Get the data.
						IfcModel subModel = new IfcModel(packageMetaData);
						session.getMap(subModel, query);
						Date date = subRevision.getDate();
						subModel.getModelMetaData().setDate(date);
						// Perform and store the counts.
						Object key = (isByRevision) ? revisionId : date;
						if (!breakdownByChoice.containsKey(key))
							breakdownByChoice.put(key, new IfcRootBreakdown());
						IfcRootBreakdown breakdown = breakdownByChoice.get(key);
						breakdown.integrateIfcModel(subModel);
						// Clear the data.
						subModel.clear();
					} catch (BimserverDatabaseException e) {
						e.printStackTrace();
					}
				}
			}
		}
		//
		if (session != null)
			session.close();
		// Add an extra entry if this is a single revision project.
		int initialSize = breakdownByChoice.size();
		if (initialSize == 1) {
			Set<Object> keys = breakdownByChoice.keySet();
			Object firstKey = keys.iterator().next();
			if (isByRevision)
			{
				Long revisionId = (Long)firstKey + 1L;
				breakdownByChoice.put(revisionId, breakdownByChoice.get(firstKey));
			} else {
				Date revisionDate = (Date)firstKey;
				revisionDate = new Date(revisionDate.getDate() + 1);
				breakdownByChoice.put(revisionDate, breakdownByChoice.get(firstKey));
			}
		}
		Set<Entry<Object, IfcRootBreakdown>> unsortedEntrySet = breakdownByChoice.entrySet();
		ArrayList<Entry<Object, IfcRootBreakdown>> sortedEntrySet = new ArrayList<>(unsortedEntrySet);
		Collections.sort(sortedEntrySet, sortLargerKeysToEnd);
		//
		IfcRootBreakdown previous = null;
		for (int i = 0; i < sortedEntrySet.size(); i++) {
			Entry<Object, IfcRootBreakdown> entry = sortedEntrySet.get(i);
			Object key = entry.getKey();
			IfcRootBreakdown value = entry.getValue();
			//
			long properties = 0, relations = 0, objects = 0, miscellaneous = 0;
			if (previous == null && useDelta) {
				// Do nothing as there's been no change (unless it's requested that deltas start from zero).
				if (startDeltaFromZero) {
					properties += value.IfcPropertyDefinitionsCount;
					relations += value.IfcRelationshipsCount;
					objects += value.IfcObjectDefinitionsCount;
					miscellaneous += value.MiscellaneousCount;
				}
			} else if (previous == null || !useDelta) {
				properties += value.IfcPropertyDefinitionsCount;
				relations += value.IfcRelationshipsCount;
				objects += value.IfcObjectDefinitionsCount;
				miscellaneous += value.MiscellaneousCount;
			} else {
				properties += value.IfcPropertyDefinitionsCount - previous.IfcPropertyDefinitionsCount;
				relations += value.IfcRelationshipsCount - previous.IfcRelationshipsCount;
				objects += value.IfcObjectDefinitionsCount - previous.IfcObjectDefinitionsCount;
				miscellaneous += value.MiscellaneousCount - previous.MiscellaneousCount;
			}
			{
				LinkedHashMap<String, Object> thisEntry = new LinkedHashMap<>();
				//
				thisEntry.put("group", "Property Definitions");
				thisEntry.put("date", key);
				thisEntry.put("size", properties);
				//
				rawData.add(thisEntry);
			}
			{
				LinkedHashMap<String, Object> thisEntry = new LinkedHashMap<>();
				//
				//
				thisEntry.put("group", "Relationships");
				thisEntry.put("date", key);
				thisEntry.put("size", relations);
				//
				rawData.add(thisEntry);
			}
			{
				LinkedHashMap<String, Object> thisEntry = new LinkedHashMap<>();
				//
				thisEntry.put("group", "Object Definitions");
				thisEntry.put("date", key);
				thisEntry.put("size", objects);
				//
				rawData.add(thisEntry);
			}
			{
				LinkedHashMap<String, Object> thisEntry = new LinkedHashMap<>();
				//
				thisEntry.put("group", "Everything Else");
				thisEntry.put("date", key);
				thisEntry.put("size", miscellaneous);
				//
				rawData.add(thisEntry);
			}
			//
			if (initialSize != 1)
				previous = value;
		}
	}

	public Revision getRevisionByRevisionId(DatabaseSession session, long rid) {
		Revision revision = null;
		try {
			revision = session.get(StorePackage.eINSTANCE.getRevision(), rid, Query.getDefault());
		} catch (BimserverDatabaseException e) {
			e.printStackTrace();
		}
		return revision;
	}

	public DatabaseSession getNewDatabaseSession(PluginManager pluginManager) {
		DatabaseSession session = null;
		try {
			DirectBimServerClientFactory bimServerClientFactory = (DirectBimServerClientFactory) FieldUtils.readField(pluginManager, "bimServerClientFactory", true);
			PublicInterfaceFactory serviceFactory = (PublicInterfaceFactory) FieldUtils.readField(bimServerClientFactory, "serviceFactory", true);
			BimServer bimServer = (BimServer) FieldUtils.readField(serviceFactory, "bimServer", true);
			session = bimServer.getDatabase().createSession();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		return session;
	}

	@Override
	protected boolean write(OutputStream outputStream) throws SerializerException {
		if (getMode() == Mode.BODY) {
			// Write chart.
			PrintWriter writer = new UTF8PrintWriter(outputStream);
			try {
				writer.print(chart.writeSVG(rawData));
				writer.flush();
			} catch (Exception e) {
				LOGGER.error("", e);
			}
			writer.close();
			setMode(Mode.FINISHED);
			return true;
		} else if (getMode() == Mode.FINISHED)
			return false;
		//
		return false;
	}

}
