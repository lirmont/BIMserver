package org.bimserver.satellite.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.bimserver.client.notifications.NotificationInterfaceAdapter;
import org.bimserver.client.notifications.NotificationLogger;
import org.bimserver.interfaces.objects.SNewRevisionNotification;
import org.bimserver.interfaces.objects.SRevision;
import org.bimserver.satellite.SatelliteServer;
import org.bimserver.satellite.SatelliteSettings;
import org.bimserver.shared.ConnectDisconnectListener;
import org.bimserver.shared.exceptions.ServiceException;
import org.bimserver.utils.SwingUtil;

public class SatelliteGui extends JFrame {

	private static final long serialVersionUID = -5428454520760923784L;
	protected static final String APP_NAME = "BIMserver Satellite";
	private SatelliteServer satelliteServer;
	private JButton connectDisconnectButton;
	private JTextArea logTextArea;
	private JTextArea notificationsTextArea;
	private SatelliteSettings settings;

	public static void main(String[] args) {
		new SatelliteGui();
	}

	public SatelliteGui() {
		SwingUtil.setLookAndFeelToNice();
		try {
			setIconImage(ImageIO.read(getClass().getResource("logo_small.png")));
		} catch (IOException e) {
		}
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(640, 480);
		setTitle(APP_NAME);
		setVisible(true);
		getContentPane().setLayout(new BorderLayout());

		File settingsFile = new File("settings.xml");
		if (settingsFile.exists()) {
			try {
				JAXBContext jaxbContext = JAXBContext.newInstance(SatelliteSettings.class);
				Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
				settings = (SatelliteSettings)unmarshaller.unmarshal(settingsFile);
			} catch (JAXBException e) {
				e.printStackTrace();
			}
		}
		if (settings == null) {
			settings = new SatelliteSettings();
		}
		
		JPanel top = new JPanel(new FlowLayout(FlowLayout.LEADING));

		connectDisconnectButton = new JButton("Connect");

		connectDisconnectButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (connectDisconnectButton.getText().equals("Connect")) {
					ConnectFrame connectFrame = new ConnectFrame(SatelliteGui.this, settings);
					connectFrame.setVisible(true);
				} else {
					satelliteServer.disconnect();
				}
			}
		});

		top.add(connectDisconnectButton);

		getContentPane().add(top, BorderLayout.NORTH);
		JTabbedPane tabber = new JTabbedPane();
		getContentPane().add(tabber, BorderLayout.CENTER);

		notificationsTextArea = new JTextArea();
		tabber.addTab("Notifications", new JScrollPane(notificationsTextArea));

		logTextArea = new JTextArea();
		tabber.addTab("Log", new JScrollPane(logTextArea));
	}

	public SatelliteServer getSatelliteServer() {
		return satelliteServer;
	}

	public void connect(SatelliteSettings settings) throws IOException {
		satelliteServer = new SatelliteServer();
		satelliteServer.getBimServerClient().registerConnectDisconnectListener(new ConnectDisconnectListener() {
			@Override
			public void connected() {
				connectDisconnectButton.setText("Disconnect");
				logTextArea.append("Connected\n");
			}

			@Override
			public void disconnected() {
				connectDisconnectButton.setText("Connect");
				logTextArea.append("Disconnected\n");
			}
		});
		satelliteServer.getNotificationsClient().registerConnectDisconnectListener(new ConnectDisconnectListener() {
			@Override
			public void disconnected() {
				notificationsTextArea.append("Connected\n");
			}
			
			@Override
			public void connected() {
				notificationsTextArea.append("Disconnected\n");
			}
		});
//		NotificationInterfaceAdapter notificationInterface = new NotificationInterfaceAdapter(){
//			@Override
//			public void newRevision(SNewRevisionNotification newRevisionNotification) throws ServiceException {
//				long roid = newRevisionNotification.getRevisionId();
//				SRevision revision = bimServerClient.getServiceInterface().getRevision(roid);
//				session.loadModel(revision);
//			}
//		};
		satelliteServer.connect(settings, new NotificationLogger(new PrintWriter(new OutputStream() {
			
			@Override
			public void write(int b) throws IOException {
				notificationsTextArea.append(new String(new char[]{(char)b}));
			}
		})));
	}
}