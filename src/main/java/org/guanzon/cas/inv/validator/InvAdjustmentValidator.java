/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.guanzon.cas.inv.validator;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.guanzon.appdriver.base.GRiderCAS;
import org.guanzon.appdriver.iface.GValidator;
import org.guanzon.cas.inv.model.Model_Inv_Adjustment_Detail;
import org.guanzon.cas.inv.model.Model_Inv_Adjustment_Master;
import org.guanzon.cas.inv.model.Model_Inv_Adjustment_Serial;
import org.guanzon.cas.inv.status.InvAdjustmentStatus;
import org.json.simple.JSONObject;
/**
 *
 * @author Arsiela 
 */
public class InvAdjustmentValidator implements GValidator{
    GRiderCAS poGRider;
    String psTranStat;
    JSONObject poJSON;
    
    Model_Inv_Adjustment_Master poMaster;
    List<Model_Inv_Adjustment_Detail> paDetail;
    List<Model_Inv_Adjustment_Serial> paSerial;

    @Override
    public void setApplicationDriver(Object applicationDriver) {
        poGRider = (GRiderCAS) applicationDriver;
    }

    @Override
    public void setTransactionStatus(String transactionStatus) {
        psTranStat = transactionStatus;
    }

    @Override
    public void setMaster(Object value) {
        poMaster = (Model_Inv_Adjustment_Master) value;
    }

    @Override
    public void setDetail(ArrayList<Object> value) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setOthers(ArrayList<Object> value) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    public void setInvDetails(List<Model_Inv_Adjustment_Detail> value) {
        paDetail = value;
    }
    
    public void setInvSerials(List<Model_Inv_Adjustment_Serial> value) {
        paSerial = value;
    }

    @Override
    public JSONObject validate() {
        try {
            switch (psTranStat){
                case InvAdjustmentStatus.OPEN:
                    return validateNew();
                case InvAdjustmentStatus.CONFIRMED:
                    return validateApproved();
                case InvAdjustmentStatus.VOID:
                    return validateVoid();
                case InvAdjustmentStatus.CANCELLED:
                    return validateCancelled();
                default:
                    poJSON = new JSONObject();
                    poJSON.put("result", "success");
            }
        } catch (SQLException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
        }
        
        return poJSON;
    }
    
    private JSONObject validateNew() throws SQLException{
        poJSON = new JSONObject();
        Date loTransactionDate = poMaster.getTransactionDate();
        if (checkEmpty(poMaster.getTransactionNo())) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction no must not be empty.");
            return poJSON;
        } 
        
        if (checkEmpty(poMaster.getCompanyId())) {
            poJSON.put("result", "error");
            poJSON.put("message", "Company must not be empty.");
            return poJSON;
        } 
        
        if (checkEmpty(poMaster.getIndustryCode())) {
            poJSON.put("result", "error");
            poJSON.put("message", "Industry must not be empty.");
            return poJSON;
        } 
        
        if (checkEmpty(poMaster.getBranchCode())) {
            poJSON.put("result", "error");
            poJSON.put("message", "Branch must not be empty.");
            return poJSON;
        } 
        
        if (checkEmpty(poMaster.getCategoryCode())) {
            poJSON.put("result", "error");
            poJSON.put("message", "Category must not be empty.");
            return poJSON;
        } 
        
        if (loTransactionDate == null) {
            poJSON.put("result", "error");
            poJSON.put("message", "Invalid transaction date.");
            return poJSON;
        }

        if ("1900-01-01".equals(xsDateShort(loTransactionDate))) {
            poJSON.put("result", "error");
            poJSON.put("message", "Invalid transaction date.");
            return poJSON;
        }
        
        if (checkEmpty(poMaster.getRemarks())) {
            poJSON.put("result", "error");
            poJSON.put("message", "Remarks is not set.");
            return poJSON;
        }
        
        int lnRow = 0;
        for(Model_Inv_Adjustment_Detail loDetail : paDetail){
            if(loDetail.isReverse()){
                lnRow++;
            }
            if(checkNotEmpty(loDetail.getStockId()) && loDetail.isReverse() ){
                if(loDetail.getCreditQuantity() > 0
                    && loDetail.getDebitQuantity() > 0){
                    poJSON.put("result", "error");
                    poJSON.put("message", "Credit and Debit quantities cannot both be specified "
                                            + "at row : " + lnRow
                                            + "./nPlease enter a value in only one of these fields.");
                    return poJSON;
                }
                if(loDetail.getInventoryCost() <= 0.00){
                    poJSON.put("result", "error");
                    poJSON.put("message", "Invalid cost."
                                            + "at row : " + lnRow
                                            + ".");
                    return poJSON;
                }
            }
        
        }
        
        poJSON.put("result", "success");
        return poJSON;
    }
    
    private JSONObject validateApproved()throws SQLException{
        poJSON = new JSONObject();
        
        poJSON = validateNew();
        if("error".equals((String) poJSON.get("result"))){
            return poJSON;
        }
        
        poJSON.put("result", "success");
        return poJSON;
    }
    
    private JSONObject validateVoid(){
        poJSON = new JSONObject();
                
        poJSON.put("result", "success");
        return poJSON;
    }
    
    private JSONObject validateCancelled(){
        poJSON = new JSONObject();
                
        poJSON.put("result", "success");
        return poJSON;
    }
    
    private static String xsDateShort(Date fdValue) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String date = sdf.format(fdValue);
        return date;
    }
    
    private LocalDate strToDate(String val) {
        DateTimeFormatter date_formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate localDate = LocalDate.parse(val, date_formatter);
        return localDate;
    }
    
    private boolean checkEmpty(String fsValue){
        return fsValue == null || "".equals(fsValue);
    }
    private boolean checkNotEmpty(String fsValue){
        return fsValue != null && !"".equals(fsValue);
    }
    
}
