/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package org.bimserver.models.ifc2x3;


/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Ifc Rel Assigns To Actor</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 *   <li>{@link org.bimserver.models.ifc2x3.IfcRelAssignsToActor#getRelatingActor <em>Relating Actor</em>}</li>
 *   <li>{@link org.bimserver.models.ifc2x3.IfcRelAssignsToActor#getActingRole <em>Acting Role</em>}</li>
 * </ul>
 * </p>
 *
 * @see org.bimserver.models.ifc2x3.Ifc2x3Package#getIfcRelAssignsToActor()
 * @model
 * @generated
 */
public interface IfcRelAssignsToActor extends IfcRelAssigns {
	/**
	 * Returns the value of the '<em><b>Relating Actor</b></em>' reference.
	 * It is bidirectional and its opposite is '{@link org.bimserver.models.ifc2x3.IfcActor#getIsActingUpon <em>Is Acting Upon</em>}'.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Relating Actor</em>' reference isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Relating Actor</em>' reference.
	 * @see #setRelatingActor(IfcActor)
	 * @see org.bimserver.models.ifc2x3.Ifc2x3Package#getIfcRelAssignsToActor_RelatingActor()
	 * @see org.bimserver.models.ifc2x3.IfcActor#getIsActingUpon
	 * @model opposite="IsActingUpon"
	 * @generated
	 */
	IfcActor getRelatingActor();

	/**
	 * Sets the value of the '{@link org.bimserver.models.ifc2x3.IfcRelAssignsToActor#getRelatingActor <em>Relating Actor</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Relating Actor</em>' reference.
	 * @see #getRelatingActor()
	 * @generated
	 */
	void setRelatingActor(IfcActor value);

	/**
	 * Returns the value of the '<em><b>Acting Role</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Acting Role</em>' reference isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Acting Role</em>' reference.
	 * @see #isSetActingRole()
	 * @see #unsetActingRole()
	 * @see #setActingRole(IfcActorRole)
	 * @see org.bimserver.models.ifc2x3.Ifc2x3Package#getIfcRelAssignsToActor_ActingRole()
	 * @model unsettable="true"
	 * @generated
	 */
	IfcActorRole getActingRole();

	/**
	 * Sets the value of the '{@link org.bimserver.models.ifc2x3.IfcRelAssignsToActor#getActingRole <em>Acting Role</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Acting Role</em>' reference.
	 * @see #isSetActingRole()
	 * @see #unsetActingRole()
	 * @see #getActingRole()
	 * @generated
	 */
	void setActingRole(IfcActorRole value);

	/**
	 * Unsets the value of the '{@link org.bimserver.models.ifc2x3.IfcRelAssignsToActor#getActingRole <em>Acting Role</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isSetActingRole()
	 * @see #getActingRole()
	 * @see #setActingRole(IfcActorRole)
	 * @generated
	 */
	void unsetActingRole();

	/**
	 * Returns whether the value of the '{@link org.bimserver.models.ifc2x3.IfcRelAssignsToActor#getActingRole <em>Acting Role</em>}' reference is set.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return whether the value of the '<em>Acting Role</em>' reference is set.
	 * @see #unsetActingRole()
	 * @see #getActingRole()
	 * @see #setActingRole(IfcActorRole)
	 * @generated
	 */
	boolean isSetActingRole();

} // IfcRelAssignsToActor