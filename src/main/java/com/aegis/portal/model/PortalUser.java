package com.aegis.portal.model;


import java.util.Set;

public class PortalUser {
	private String usrID;
	private String workOrgID;
	private String salutation;
	private String firstName;
	private String middleName;
	private String lastName;
	private String displayName;
	private String title;
	private String orgName;
	private String mobileNo;
	private String workNo;
	private String xAEGISNo;
	private String email;
	private String userName;
	private String password;
	private String pwdModFlg;
	private String member;
	private String activeFlg;
	private String termsviewed;
	private String networkID;
	private String externalCompanyName;
	private String externalCompanyId;


	public String getExternalCompanyName() {
		return externalCompanyName;
	}
	public void setExternalCompanyName(String externalCompanyName) {
		this.externalCompanyName = externalCompanyName;
	}
	public String getExternalCompanyId() {
		return externalCompanyId;
	}
	public void setExternalCompanyId(String externalCompanyId) {
		this.externalCompanyId = externalCompanyId;
	}
	public String getTermsviewed() {
		return termsviewed;
	}
	public void setTermsviewed(String termsviewed) {
		this.termsviewed = termsviewed;
	}
	private Set<String> groups;
	public String getUsrID() {
		return usrID;
	}
	public void setUsrID(String usrID) {
		this.usrID = usrID;
	}
	public String getWorkOrgID() {
		return workOrgID;
	}
	public void setWorkOrgID(String workOrgID) {
		this.workOrgID = workOrgID;
	}
	public String getSalutation() {
		return salutation;
	}
	public void setSalutation(String salutation) {
		this.salutation = salutation;
	}

	public String getFirstName() {
		return firstName;
	}
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}
	public String getLastName() {
		return lastName;
	}
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getUserName() {
		return userName;
	}
	public void setUserName(String userName) {
		this.userName = userName;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public Set<String> getGroups() {
		return groups;
	}
	@Override
	public String toString() {
		return "PortalUser [" + (usrID != null ? "usrID=" + usrID + ", " : "")
				+ (workOrgID != null ? "workOrgID=" + workOrgID + ", " : "")
				+ (salutation != null ? "salutation=" + salutation + ", " : "")
				+ (firstName != null ? "firstName=" + firstName + ", " : "")
				+ (middleName != null ? "middleName=" + middleName + ", " : "")
				+ (lastName != null ? "lastName=" + lastName + ", " : "")
				+ (displayName != null ? "displayName=" + displayName + ", " : "")
				+ (title != null ? "title=" + title + ", " : "") + (orgName != null ? "orgName=" + orgName + ", " : "")
				+ (mobileNo != null ? "mobileNo=" + mobileNo + ", " : "")
				+ (workNo != null ? "workNo=" + workNo + ", " : "")
				+ (xAEGISNo != null ? "xAEGISNo=" + xAEGISNo + ", " : "")
				+ (email != null ? "email=" + email + ", " : "")
				+ (userName != null ? "userName=" + userName + ", " : "")
				+ (password != null ? "password=" + password + ", " : "")
				+ (pwdModFlg != null ? "pwdModFlg=" + pwdModFlg + ", " : "")
				+ (member != null ? "member=" + member + ", " : "")
				+ (activeFlg != null ? "activeFlg=" + activeFlg + ", " : "")
				+ (groups != null ? "groups=" + groups : "") + "]";
	}
	public void setGroups(Set<String> groups) {
		this.groups = groups;
	}
	public String getMiddleName() {
		return middleName;
	}
	public void setMiddleName(String middleName) {
		this.middleName = middleName;
	}
	public String getDisplayName() {
		return displayName;
	}
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getOrgName() {
		return orgName;
	}
	public void setOrgName(String orgName) {
		this.orgName = orgName;
	}
	public String getMobileNo() {
		return mobileNo;
	}
	public void setMobileNo(String mobileNo) {
		this.mobileNo = mobileNo;
	}
	public String getWorkNo() {
		return workNo;
	}
	public void setWorkNo(String workNo) {
		this.workNo = workNo;
	}
	public String getxAEGISNo() {
		return xAEGISNo;
	}
	public void setxAEGISNo(String xAEGISNo) {
		this.xAEGISNo = xAEGISNo;
	}	
	public String getPwdModFlg() {
		return pwdModFlg;
	}
	public void setPwdModFlg(String pwdModFlg) {
		this.pwdModFlg = pwdModFlg;
	}

	public String getActiveFlg() {
		return activeFlg;
	}
	public void setActiveFlg(String activeFlg) {
		this.activeFlg = activeFlg;
	}
	public String getMember() {
		return member;
	}
	public void setMember(String member) {
		this.member = member;
	}
	public String getNetworkID() {
		return networkID;
	}
	public void setNetworkID(String networkID) {
		this.networkID = networkID;
	}

}
