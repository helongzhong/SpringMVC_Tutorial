package payment.enrollment.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.sf.json.JSONObject;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;

import payment.enrollment.dao.EnrollmentUserDAO;
import payment.enrollment.domain.Tx2711Request;
import payment.enrollment.domain.Tx2711Response;
import payment.enrollment.entity.CertificateApply;
import payment.enrollment.entity.CorpBasic;
import payment.enrollment.entity.EnrollmentUser;
import payment.enrollment.entity.SignRecord;
import payment.enrollment.entity.SignerInfos;
import payment.enrollment.service.ContractService;
import payment.enrollment.service.common.ContractImportService1;
import payment.enrollment.service.common.ContractImportService2;
import payment.enrollment.service.common.ContractImportService3;
import payment.enrollment.service.common.ContractImportService4;
import payment.enrollment.system.SystemEnvironment;
import payment.enrollment.system.TxMessenger;
import payment.tools.util.GUID;

/**
 * <pre>
 * Modify Information:
 * Author       Date          Description
 * ============ ============= ============================
 * wangqingfeng     2017年11月17日   create this file
 * </pre>
 */
@Service
public class SignContractService2 {
    private static final Logger logger = Logger.getLogger("service");

    @Autowired
    private EnrollmentUserDAO enrollmentUserDAO;

    @Autowired
    private ContractService contractService;

    @Autowired
    private ContractImportService2 contractImportService2;
    
    @Autowired
    private ContractImportService1 contractImportService1;
    
    @Autowired
    private ContractImportService3 contractImportService3;
    
    @Autowired
    private ContractImportService4 contractImportService4;

    private static final String SIGNSUCCESSSTR = "signSuccess";

    /**
     * <p>
     * 协议签署报文发送具体实现（如果捕获异常返回""，成功返回"signSuccess"）
     * </p>
     * 
     * @author zhanghaizhao
     * @date 2017年11月3日 下午5:18:13
     * @param corpBasic
     * @return
     */
    public String sendSignMessage(CorpBasic corpBasic) {
        try {
            // 报文组装和发送
            String contractID = corpBasic.getContractID();
            
            // 发送业务合作协议
            List<SignRecord> signRecordMOList = contractService.findSignRecord(contractID, "JS_504");
            Tx2711Request tx2711Request4MO = getTx2711Request4MO(corpBasic);
            if(signRecordMOList == null || signRecordMOList.isEmpty()){
                contractService.addSignRecord(contractID, tx2711Request4MO.getTemplateID(), "");
                sendAndProsses(tx2711Request4MO, corpBasic, "");
            }else if(signRecordMOList.get(0).getStatus() == 10){
                sendAndProsses(tx2711Request4MO, corpBasic, "");
            }
            
            // 发送上线协议
            List<SignRecord> signRecordOLList = contractService.findSignRecord(contractID, "JS_502");
            Tx2711Request tx2711Request4OL = getTx2711Request4OL(corpBasic);
            if(signRecordOLList == null || signRecordOLList.isEmpty()){
                contractService.addSignRecord(contractID, tx2711Request4OL.getTemplateID(), "");
                sendAndProsses(tx2711Request4OL, corpBasic, "");
            }else if(signRecordOLList.get(0).getStatus() == 10){
                sendAndProsses(tx2711Request4OL, corpBasic, "");
            }
            
            // 发送企业协议
            List<SignRecord> signRecordCorpList = contractService.findSignRecord(contractID, "JS_505");
            List<CertificateApply> corpList = contractService.getCertificateApplyList(contractID, 20);
            Tx2711Request tx2711Request4Corp = getTx2711Request4Corp(corpBasic);
            if(signRecordCorpList == null || signRecordCorpList.isEmpty()){
                contractService.addSignRecord(contractID, tx2711Request4Corp.getTemplateID(), corpList.get(0).getSystemNo());
                sendAndProsses(tx2711Request4Corp, corpBasic, corpList.get(0).getSystemNo());
            }else if(signRecordCorpList.get(0).getStatus() == 10){
                sendAndProsses(tx2711Request4Corp, corpBasic, corpList.get(0).getSystemNo());
            }
            
            
            // 发送个人协议
            List<SignRecord> signRecordPerList = contractService.findSignRecord(contractID, "JS_503");
            List<CertificateApply> certificateApplyCorpList = contractService.getCertificateApplyList(contractID, 10);
            if(certificateApplyCorpList != null && !certificateApplyCorpList.isEmpty()){
                for (int i = 0; i < certificateApplyCorpList.size(); i++) {
                    CertificateApply certificateApply = certificateApplyCorpList.get(i);
                    Tx2711Request tx2711Request4Per = getTx2711Request4Per(corpBasic, certificateApply);
                    if(signRecordPerList == null || signRecordPerList.isEmpty()){
                        contractService.addSignRecord(contractID, tx2711Request4Per.getTemplateID(), certificateApply.getSystemNo());
                        sendAndProsses(tx2711Request4Per, corpBasic, certificateApply.getSystemNo());
                    }else if(signRecordPerList.get(i).getStatus() == 10){
                        sendAndProsses(tx2711Request4Per, corpBasic, certificateApply.getSystemNo());
                    }
                }
            }
            
            List<SignRecord> signRecordList = contractService.findSignRecord(contractID, "");
            if(signRecordList != null && !signRecordList.isEmpty()){
                for(SignRecord s :signRecordList){
                    if(s.getStatus() == 10){
                        return "";
                    }
                }
            }
            return SIGNSUCCESSSTR;
        } catch (Exception e) {
            logger.error("签署协议失败", e);
            return "";
        }
    }

    /**
     * <p>
     * 组装2711报文数据-线上协议
     * </p>
     */
    public Tx2711Request getTx2711Request4OL(CorpBasic corpBasic) throws Exception {
        Map<String, String> tMap = contractImportService2.contractFillField(corpBasic.getContractID());
        JSONObject json = JSONObject.fromObject(tMap);
        EnrollmentUser user = enrollmentUserDAO.selectUserByUserID(corpBasic.getAdminID());
        Tx2711Request tx2711Request = new Tx2711Request();
        tx2711Request.setInstitutionID(SystemEnvironment.institutionID);
        // tx2711Request.setInstitutionID(corpBasic.getInstitutionID());
        tx2711Request.setTxSN(GUID.genTxNo(25));
        tx2711Request.setTemplateID("JS_502");
        tx2711Request.setContractInfos(json.toString());
        SignerInfos signerInfo1 = new SignerInfos();
        signerInfo1.setAccountType("12");
        signerInfo1.setName(corpBasic.getMerchantName());
        signerInfo1.setIdentificationType("B");
        if (corpBasic.getIsTriInOne() == 1) {
            signerInfo1.setIdentificationNumber(corpBasic.getCreditCode());
        } else {
            signerInfo1.setIdentificationNumber(corpBasic.getLicenseNo());
        }
        signerInfo1.setEmail(user.getLoginName());
        signerInfo1.setLandlinePhone(user.getPhoneNumber());
        signerInfo1.setAddress(corpBasic.getRegisterAddress());
        signerInfo1.setOperatorName(user.getUserName());
        signerInfo1.setOperatorIdentType(user.getIdentificationType());
        signerInfo1.setOperatorIdentNumber(user.getIdentificationNumber());
        signerInfo1.setSignlocation("S1");
        signerInfo1.setsMSFlag("0");

        List<SignerInfos> signerInfos = new ArrayList<SignerInfos>();
        signerInfos.add(signerInfo1);
        tx2711Request.setSignerInfos(signerInfos);
        tx2711Request.process();
        return tx2711Request;
    }
    
    /**
     * <p>
     * 组装2711报文数据-企业协议
     * </p>
     */
    public Tx2711Request getTx2711Request4Corp(CorpBasic corpBasic) throws Exception {
        Map<String, String> tMap = contractImportService3.contractFillField(corpBasic.getContractID());
        JSONObject json = JSONObject.fromObject(tMap);
        EnrollmentUser user = enrollmentUserDAO.selectUserByUserID(corpBasic.getAdminID());
        Tx2711Request tx2711Request = new Tx2711Request();
        tx2711Request.setInstitutionID(SystemEnvironment.institutionID);
        // tx2711Request.setInstitutionID(corpBasic.getInstitutionID());
        tx2711Request.setTxSN(GUID.genTxNo(25));
        tx2711Request.setTemplateID("JS_505");
        tx2711Request.setContractInfos(json.toString());
        SignerInfos signerInfo1 = new SignerInfos();
        signerInfo1.setAccountType("12");
        signerInfo1.setName(corpBasic.getMerchantName());
        signerInfo1.setIdentificationType("B");
        if (corpBasic.getIsTriInOne() == 1) {
            signerInfo1.setIdentificationNumber(corpBasic.getCreditCode());
        } else {
            signerInfo1.setIdentificationNumber(corpBasic.getLicenseNo());
        }
        signerInfo1.setEmail(user.getLoginName());
        signerInfo1.setLandlinePhone(user.getPhoneNumber());
        signerInfo1.setAddress(corpBasic.getRegisterAddress());
        signerInfo1.setOperatorName(user.getUserName());
        signerInfo1.setOperatorIdentType(user.getIdentificationType());
        signerInfo1.setOperatorIdentNumber(user.getIdentificationNumber());
        signerInfo1.setSignlocation("S1");
        signerInfo1.setsMSFlag("0");

        List<SignerInfos> signerInfos = new ArrayList<SignerInfos>();
        signerInfos.add(signerInfo1);
        tx2711Request.setSignerInfos(signerInfos);
        tx2711Request.process();
        return tx2711Request;
    }
    
    /**
     * <p>
     * 组装2711报文数据-个人协议
     * </p>
     */
    public Tx2711Request getTx2711Request4Per(CorpBasic corpBasic, CertificateApply certificateApply) throws Exception {
        Map<String, String> tMap = contractImportService4.contractFillField(corpBasic.getContractID(), certificateApply);
        JSONObject json = JSONObject.fromObject(tMap);
        EnrollmentUser user = enrollmentUserDAO.selectUserByUserID(corpBasic.getAdminID());
        Tx2711Request tx2711Request = new Tx2711Request();
        tx2711Request.setInstitutionID(SystemEnvironment.institutionID);
        // tx2711Request.setInstitutionID(corpBasic.getInstitutionID());
        tx2711Request.setTxSN(GUID.genTxNo(25));
        tx2711Request.setTemplateID("JS_503");
        tx2711Request.setContractInfos(json.toString());
        SignerInfos signerInfo1 = new SignerInfos();
        signerInfo1.setAccountType("12");
        signerInfo1.setName(corpBasic.getMerchantName());
        signerInfo1.setIdentificationType("B");
        if (corpBasic.getIsTriInOne() == 1) {
            signerInfo1.setIdentificationNumber(corpBasic.getCreditCode());
        } else {
            signerInfo1.setIdentificationNumber(corpBasic.getLicenseNo());
        }
        signerInfo1.setEmail(user.getLoginName());
        signerInfo1.setLandlinePhone(user.getPhoneNumber());
        signerInfo1.setAddress(corpBasic.getRegisterAddress());
        signerInfo1.setOperatorName(user.getUserName());
        signerInfo1.setOperatorIdentType(user.getIdentificationType());
        signerInfo1.setOperatorIdentNumber(user.getIdentificationNumber());
        signerInfo1.setSignlocation("S1");
        signerInfo1.setsMSFlag("0");

        List<SignerInfos> signerInfos = new ArrayList<SignerInfos>();
        signerInfos.add(signerInfo1);
        tx2711Request.setSignerInfos(signerInfos);
        
        tx2711Request.process();
        return tx2711Request;
    }
    
    /**
     * <p>
     * 组装2711报文数据-业务服务合作协议
     * </p>
     */
    public Tx2711Request getTx2711Request4MO(CorpBasic corpBasic) throws Exception {
        Map<String, String> tMap = contractImportService1.contractFillField(corpBasic.getContractID());
        JSONObject json = JSONObject.fromObject(tMap);
        EnrollmentUser user = enrollmentUserDAO.selectUserByUserID(corpBasic.getAdminID());
        EnrollmentUser currentUser = enrollmentUserDAO.selectUserByUserID(corpBasic.getCreateUserID());
        Tx2711Request tx2711Request = new Tx2711Request();
        tx2711Request.setInstitutionID(SystemEnvironment.institutionID);
        // tx2711Request.setInstitutionID(corpBasic.getInstitutionID());
        tx2711Request.setTxSN(GUID.genTxNo(25));
        tx2711Request.setTemplateID("JS_504");
        tx2711Request.setContractInfos(json.toString());
        SignerInfos signerInfo1 = new SignerInfos();
        signerInfo1.setAccountType("12");
        signerInfo1.setName(corpBasic.getMerchantName());
        signerInfo1.setIdentificationType("B");
        if (corpBasic.getIsTriInOne() == 1) {
            signerInfo1.setIdentificationNumber(corpBasic.getCreditCode());
        } else {
            signerInfo1.setIdentificationNumber(corpBasic.getLicenseNo());
        }
        signerInfo1.setEmail(user.getLoginName());
        signerInfo1.setLandlinePhone(user.getPhoneNumber());
        signerInfo1.setAddress(corpBasic.getRegisterAddress());
        signerInfo1.setOperatorName(user.getUserName());
        signerInfo1.setOperatorIdentType(user.getIdentificationType());
        signerInfo1.setOperatorIdentNumber(user.getIdentificationNumber());
        signerInfo1.setSignlocation("S2");
        signerInfo1.setsMSFlag("0");

        List<SignerInfos> signerInfos = new ArrayList<SignerInfos>();
        signerInfos.add(signerInfo1);
        SignerInfos cfcaInfo = SystemEnvironment.signerInfos;
        cfcaInfo.setOperatorName(currentUser.getUserName());
        cfcaInfo.setOperatorIdentType(currentUser.getIdentificationType());
        cfcaInfo.setOperatorIdentNumber(currentUser.getIdentificationNumber());
        signerInfos.add(cfcaInfo);
        tx2711Request.setSignerInfos(signerInfos);

        tx2711Request.process();
        return tx2711Request;
    }
    
    public void sendAndProsses(Tx2711Request tx2711Request, CorpBasic corpBasic, String sourceSystemNo) throws Exception {
        // 报文组装和发送
        String contractID = corpBasic.getContractID();
        TxMessenger txMessenger = new TxMessenger();
        
        Document document = txMessenger.send(tx2711Request.getRequestMessage(), tx2711Request.getRequestSignature());
        Tx2711Response tx2711Response = new Tx2711Response();
        tx2711Response.process(document);
        if("2000".equals(tx2711Response.getCode()) && ("30").equals(tx2711Response.getStatus())){
            contractService.updateSignRecord(contractID, tx2711Request.getTemplateID(), tx2711Response.getTxSN(), 20, sourceSystemNo);
        }
    }
}
