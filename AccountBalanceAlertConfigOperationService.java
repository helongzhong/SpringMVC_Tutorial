package payment.management.service.txmonitor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.ModelAndView;

import payment.common.bo.AccountBalanceAlertConfigBO;
import payment.common.domain.txmonitor.AccountBalanceAlertConfig;
import payment.management.system.SessionUser;
import payment.tools.system.BaseOperationService;
import payment.tools.system.CodeException;
import payment.tools.util.AmountFormat;
import payment.tools.util.StringUtil;

@Service("AccountBalanceAlertConfigOperationService")
public class AccountBalanceAlertConfigOperationService extends BaseOperationService implements payment.tools.system.OperationService {

    @Resource(name = "AccountBalanceAlertConfigBO")
    private AccountBalanceAlertConfigBO accountBalanceAlertConfigBO;

    @Override
    public ModelAndView insert(HttpServletRequest request, HttpServletResponse response) throws CodeException {
        try {
            String reserveAccount = StringUtil.trim(request.getParameter("reserveAccount"));

            String[] args = reserveAccount.split("\\|");
            String bankID = args[0];
            String accountType = args[1];
            String accountNumber = args[2];
            long commonAlertThreshold = AmountFormat.yuan2Fen(Long.valueOf(StringUtil.trim(request.getParameter("commonAlertThreshold"))));
            long seriousAlertThreshold = AmountFormat.yuan2Fen(Long.valueOf(StringUtil.trim(request.getParameter("seriousAlertThreshold"))));
            
            String sql = "SELECT * FROM AccountBalanceAlertConfig WHERE bankID=? AND accountType=? AND accountNumber=?";
            AccountBalanceAlertConfig instiRespConfi=this.smartDAO.find(AccountBalanceAlertConfig.class, sql, new Object[]{bankID, accountType, accountNumber});
            if(instiRespConfi!=null){
                throw new CodeException("", "该预警配置信息已存在,请查询修改");
            }

            accountBalanceAlertConfigBO.insert(bankID, accountType, accountNumber, commonAlertThreshold, seriousAlertThreshold, SessionUser
                    .getUserName(request));

            return new ModelAndView("Success");
        } catch (CodeException e) {
            throw e;
        } catch (Exception e) {
            logger.error("", e);
            throw new CodeException("2001");
        }
    }

    @Override
    public ModelAndView edit(HttpServletRequest request, HttpServletResponse response) throws CodeException {
        try {
            String bankID = StringUtil.trim(request.getParameter("bankID"));
            String accountType = StringUtil.trim(request.getParameter("accountType"));
            String accountNumber = StringUtil.trim(request.getParameter("accountNumber"));
            long commonAlertThreshold = AmountFormat.yuan2Fen(Long.valueOf(StringUtil.trim(request.getParameter("commonAlertThreshold"))));
            long seriousAlertThreshold = AmountFormat.yuan2Fen(Long.valueOf(StringUtil.trim(request.getParameter("seriousAlertThreshold"))));

            accountBalanceAlertConfigBO.update(bankID, accountType, accountNumber, commonAlertThreshold, seriousAlertThreshold, SessionUser
                    .getUserName(request));

            return new ModelAndView("Success");
        } catch (CodeException e) {
            throw e;
        } catch (Exception e) {
            logger.error("", e);
            throw new CodeException("2001");
        }
    }

    @Override
    public ModelAndView delete(HttpServletRequest request, HttpServletResponse response) throws CodeException {

        try {
            String bankID = StringUtil.trim(request.getParameter("bankID"));
            String accountType = StringUtil.trim(request.getParameter("accountType"));
            String accountNumber = StringUtil.trim(request.getParameter("accountNumber"));

            accountBalanceAlertConfigBO.delete(bankID, accountType, accountNumber, SessionUser.getUserName(request));

            return new ModelAndView("Success");
        } catch (Exception e) {
            logger.error("", e);
            throw new CodeException("2001");
        }
    }
}
