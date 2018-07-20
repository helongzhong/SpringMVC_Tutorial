package payment.enrollment.controller;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipOutputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.AcroFields;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;

import payment.enrollment.dao.CertificateApplyDAO;
import payment.enrollment.dao.CorpBasicDAO;
import payment.enrollment.entity.CertificateApply;
import payment.enrollment.entity.CorpBasic;
import payment.enrollment.entity.ProductConfig;
import payment.enrollment.service.common.ContractImportService1;
import payment.enrollment.service.common.ContractImportService2;
import payment.enrollment.service.common.ContractImportService3;
import payment.enrollment.service.common.ContractImportService4;
import payment.enrollment.service.common.ContractImportService5;
import payment.enrollment.system.SystemEnvironment;
import payment.tools.system.CodeException;
import payment.tools.util.StringUtil;

/**
 * <pre>
 * Modify Information:
 * Author       Date          Description
 * ============ ============= ============================
 * wangqingfeng     2017年11月16日   create this file
 * </pre>
 */
@Controller
public class ContractImportController {

    protected static final Logger logger = Logger.getLogger("service");

    @Autowired
    private ContractImportService1 contractImportService1;

    @Autowired
    private ContractImportService2 contractImportService2;

    @Autowired
    private ContractImportService3 contractImportService3;

    @Autowired
    private ContractImportService4 contractImportService4;

    @Autowired
    private ContractImportService5 contractImportService5;

    @Autowired
    private CertificateApplyDAO certificateApplyDAO;

    @Autowired
    private CorpBasicDAO corpBasicDAO;

    /**
     * <p>
     * 跳转到导出协议页面
     * </p>
     * 
     * @author wangqingfeng
     * @date 2017年12月13日 下午5:48:02
     * @param request
     * @param contractID
     * @return
     * @throws CodeException
     */
    @ResponseBody
    @RequestMapping("/toImportContractPDF.do")
    public ModelAndView toImportContractPDF(HttpServletRequest request, String contractID) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("contractID", contractID);
        return new ModelAndView("7001/importPDF", map);
    }

    @RequestMapping("/importContractPDF.do")
    public void importContractPDF(HttpServletResponse response, String contractID, String contractImport) throws CodeException {
        ZipOutputStream zos = null;
        BufferedOutputStream bos = null;

        try {
            // 解析参数
            String[] contractImportArray = contractImport.substring(0, contractImport.length() - 1).split(",");
            CorpBasic corpBasic = corpBasicDAO.findCorpBasic(contractID);
            String zipName = corpBasic.getMerchantName() + contractID + ".zip";
            response.setContentType("application/x-msdownload");
            response.setHeader("content-disposition", "attachment;filename=" + URLEncoder.encode(zipName, "utf-8"));

            zos = new ZipOutputStream(response.getOutputStream());
            zos.setEncoding("UTF-8");
            bos = new BufferedOutputStream(zos);
            for (int i = 0; i < contractImportArray.length; i++) {
                if ("1".equals(contractImportArray[i])) {
                    // 产品合作协议
                    Map<String, String> contractImport1Map = contractImportService1.contractFillField(contractID);
                    String contractImport1Path = SystemEnvironment.configPath + "AggPaymentServiceContract.pdf";
                    byte[] contractImport1 = fromPDFTempletToPdfWithValue(contractImport1Path, contractImport1Map);
                    process(contractImport1, zos, bos, corpBasic.getMerchantName(), "产品合作协议.pdf");
                } else if ("2".equals(contractImportArray[i])) {
                    // 商户上线申请
                    Map<String, String> contractImport2Map = contractImportService2.contractFillField(contractID);
                    String contractImport2Path = SystemEnvironment.configPath + "MerchantOnlineContract.pdf";
                    byte[] contractImport2 = fromPDFTempletToPdfWithValue(contractImport2Path, contractImport2Map);
                    process(contractImport2, zos, bos, corpBasic.getMerchantName(), "商户上线申请表.pdf");
                } else if ("3".equals(contractImportArray[i])) {
                    // 机构证书申请
                    Map<String, String> contractImport3Map = contractImportService3.contractFillField(contractID);
                    String contractImport3Path = SystemEnvironment.configPath + "CorpCertificate.pdf";
                    byte[] contractImport3 = fromPDFTempletToPdfWithValue(contractImport3Path, contractImport3Map);
                    process(contractImport3, zos, bos, corpBasic.getMerchantName(), "企业证书申请表.pdf");
                } else if ("4".equals(contractImportArray[i])) {
                    // 个人证书申请 可能有多个
                    List<CertificateApply> list = certificateApplyDAO.findCertificateApply(contractID, 10);
                    for (CertificateApply certificateApply : list) {
                        Map<String, String> contractImport4Map = contractImportService4.contractFillField(contractID, certificateApply);
                        String contractImport4Path = SystemEnvironment.configPath + "PersonalCertificate.pdf";
                        byte[] contractImport4 = fromPDFTempletToPdfWithValue(contractImport4Path, contractImport4Map);
                        process(contractImport4, zos, bos, corpBasic.getMerchantName(), certificateApply.getName() + "个人证书申请表.pdf");
                    }
                } else if ("5".equals(contractImportArray[i])) {
                    // 产品接入方案
                    Map<String, String> contractImport5Map = contractImportService5.contractFillField(contractID);
                    String contractImport5Path = SystemEnvironment.configPath + "ProductAccessScheme.pdf";
                    byte[] contractImport5 = fromPDFTempletToPdfWithValue(contractImport5Path, contractImport5Map);
                    process(contractImport5, zos, bos, corpBasic.getMerchantName(), "产品接入方案.pdf");
                }
            }
        } catch (Exception e) {
            logger.error("", e);
            throw new CodeException("zip打包失败");
        } finally {
            if (null != zos) {
                try {
                    zos.close();
                } catch (IOException e) {
                    logger.error("", e);
                }
            }
            if (null != bos) {
                try {
                    bos.close();
                } catch (IOException e) {
                    logger.error("", e);
                }
            }
        }

    }

    public void process(byte[] btyeArray, ZipOutputStream zos, BufferedOutputStream bos, String merchantName, String contractName) throws IOException {
        BufferedInputStream bis = new BufferedInputStream(new ByteArrayInputStream(btyeArray));
        ZipEntry zipEntry = new ZipEntry(merchantName + "-" + contractName);
        zos.putNextEntry(zipEntry);
        int len = 0;
        byte[] buf = new byte[10 * 1024];
        while ((len = bis.read(buf, 0, buf.length)) != -1) {
            bos.write(buf, 0, len);
        }
        bos.flush();
        bis.close();
    }

    /**
     * <p>
     * 获取协议模板，在协议模板中填充数据
     * </p>
     * 
     * @author dell
     * @date 2017年12月14日 上午11:31:24
     * @param modelRealPath
     * @param map
     * @return
     * @throws CodeException
     */
    public byte[] fromPDFTempletToPdfWithValue(String modelRealPath, Map<String, String> map) throws CodeException {
        ByteArrayOutputStream bos = null;
        PdfStamper ps = null;
        Document document = null;
        byte[] result = null;
        PdfReader reader;
        try {
            reader = new PdfReader(modelRealPath);

            bos = new ByteArrayOutputStream();
            ps = new PdfStamper(reader, bos);
            AcroFields acroFields = ps.getAcroFields();
            for (int i = 1; i <= map.size(); i++) {
                acroFields.setField("T" + i, StringUtil.trim(map.get("T" + i)));
            }

            document = new Document();
            ps.setFormFlattening(true);
            document.open();
            PdfPTable table=new PdfPTable(4);
            PdfPCell pdfCell = new PdfPCell(); //表格的单元格
            Paragraph paragraph = new Paragraph("1111");
            pdfCell.setPhrase(paragraph);
            table.addCell(pdfCell);
            document.add(table);
            ps.close();
            result = bos.toByteArray();
            bos.close();
        } catch (FileNotFoundException e) {
            logger.error(e);
            throw new CodeException("2001", "未找到协议模板");

        } catch (Exception e) {
            logger.error(e);
            throw new CodeException("2001", "");
        } finally {
            if (null != document) {
                document.close();
            }
            try {
                if (null != ps) {
                    ps.close();
                }
                if (null != bos) {
                    bos.close();
                }
            } catch (DocumentException e) {
                logger.error(e);
            } catch (IOException e) {
                logger.error(e);
            }
        }
        // 返回一个直接数组
        return result;
    }
}
