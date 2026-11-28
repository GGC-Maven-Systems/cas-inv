package org.guanzon.cas.inv.services;

import org.guanzon.appdriver.base.GRiderCAS;
import org.guanzon.cas.inv.model.Model_Classification_Config;
import org.guanzon.cas.inv.model.Model_Inv_Adjustment_Detail;
import org.guanzon.cas.inv.model.Model_Inv_Adjustment_Master;
import org.guanzon.cas.inv.model.Model_Inv_Adjustment_Serial;
import org.guanzon.cas.inv.model.Model_Inv_Ledger;
import org.guanzon.cas.inv.model.Model_Inv_Master;
import org.guanzon.cas.inv.model.Model_Inv_Serial;
import org.guanzon.cas.inv.model.Model_Inv_Serial_Ledger;
import org.guanzon.cas.inv.model.Model_Inv_Serial_Registration;
import org.guanzon.cas.inv.model.Model_Inventory;
import org.guanzon.cas.inv.model.Model_Inventory_Child_Unit;
import org.guanzon.cas.inv.model.Model_InventorySuperseded;
import org.guanzon.cas.inv.model.Model_Inventory_Supplier;

public class InvModels {

    public InvModels(GRiderCAS applicationDriver) {
        poGRider = applicationDriver;
    }

    public Model_Inventory Inventory() {
        if (poGRider == null) {
            System.err.println("InvModels.Inventory: Application driver is not set.");
            return null;
        }

        if (poInventory == null) {
            poInventory = new Model_Inventory();
            poInventory.setApplicationDriver(poGRider);
            poInventory.setXML("Model_Inventory");
            poInventory.setTableName("Inventory");
            poInventory.initialize();
        }

        return poInventory;
    }

    public Model_InventorySuperseded InventorySuperseded() {
        if (poGRider == null) {
            System.err.println("InvModels.InventorySuperseded: Application driver is not set.");
            return null;
        }

        if (poInventory == null) {
            poInventorySuperseded = new Model_InventorySuperseded();
            poInventorySuperseded.setApplicationDriver(poGRider);
            poInventorySuperseded.setXML("Model_Inventory");
            poInventorySuperseded.setTableName("Inventory");
            poInventorySuperseded.initialize();
        }

        return poInventorySuperseded;
    }

    public Model_Inv_Master InventoryMaster() {
        if (poGRider == null) {
            System.err.println("InvModels.InventoryMaster: Application driver is not set.");
            return null;
        }

        if (poInvMaster == null) {
            poInvMaster = new Model_Inv_Master();
            poInvMaster.setApplicationDriver(poGRider);
            poInvMaster.setXML("Model_Inv_Master");
            poInvMaster.setTableName("Inv_Master");
            poInvMaster.initialize();
        }

        return poInvMaster;
    }

    public Model_Inv_Serial InventorySerial() {
        if (poGRider == null) {
            System.err.println("InvModels.InventorySerial: Application driver is not set.");
            return null;
        }

        if (poInvSerial == null) {
            poInvSerial = new Model_Inv_Serial();
            poInvSerial.setApplicationDriver(poGRider);
            poInvSerial.setXML("Model_Inv_Serial");
            poInvSerial.setTableName("Inv_Serial");
            poInvSerial.initialize();
        }

        return poInvSerial;
    }

//    public Model_Inv_Ledger InventoryLedger() {
//        if (poGRider == null) {
//            System.err.println("InvModels.InventoryLedger: Application driver is not set.");
//            return null;
//        }
//
//        if (poInvLedger == null) {
//            poInvLedger = new Model_Inv_Ledger();
//            poInvLedger.setApplicationDriver(poGRider);
//            poInvLedger.setXML("Model_Inv_Ledger");
//            poInvLedger.setTableName("Inv_Ledger");
//            poInvLedger.initialize();
//        }
//
//        return poInvLedger;
//    }
//   
//    
    public Model_Inv_Serial_Ledger InventorySerialLedger() {
        if (poGRider == null) {
            System.err.println("InvModels.InventorySerialLedger: Application driver is not set.");
            return null;
        }

        if (poInvSerialLeger == null) {
            poInvSerialLeger = new Model_Inv_Serial_Ledger();
            poInvSerialLeger.setApplicationDriver(poGRider);
            poInvSerialLeger.setXML("Model_Inv_Serial_Ledger");
            poInvSerialLeger.setTableName("Inv_Serial_Ledger");
            poInvSerialLeger.initialize();
        }

        return poInvSerialLeger;
    }

    public Model_Inv_Serial_Registration InventorySerialRegistration() {
        if (poGRider == null) {
            System.err.println("InvModels.InventorySerialRegistration: Application driver is not set.");
            return null;
        }

        if (poInvSerialReg == null) {
            poInvSerialReg = new Model_Inv_Serial_Registration();
            poInvSerialReg.setApplicationDriver(poGRider);
            poInvSerialReg.setXML("Model_Inv_Serial_Registration");
            poInvSerialReg.setTableName("Inv_Serial_Registration");
            poInvSerialReg.initialize();
        }

        return poInvSerialReg;
    }

    public Model_Classification_Config ClassificationConfig() {
        if (poGRider == null) {
            System.err.println("InvModels.ClassificationConfig: Application driver is not set.");
            return null;
        }

        if (poClassificationConfig == null) {
            poClassificationConfig = new Model_Classification_Config();
            poClassificationConfig.setApplicationDriver(poGRider);
            poClassificationConfig.setXML("Model_xxxClassificationConfig");
            poClassificationConfig.setTableName("xxxClassificationConfig");
            poClassificationConfig.initialize();
        }

        return poClassificationConfig;
    }

    public Model_Inventory_Supplier InventorySupplier() {
        if (this.poGRider == null) {
            System.err.println("DeliveryParamModels.InventorySupplier: Application driver is not set.");
            return null;
        }
        if (this.poSupplier == null) {
            this.poSupplier = new Model_Inventory_Supplier();
            this.poSupplier.setApplicationDriver(this.poGRider);
            this.poSupplier.setXML("Model_Inv_Supplier");
            this.poSupplier.setTableName("Inv_Supplier");
            this.poSupplier.initialize();
        }
        return this.poSupplier;
    }

    public Model_Inv_Ledger InventoryLedger() {
        if (this.poGRider == null) {
            System.err.println("DeliveryParamModels.InventoryLedger: Application driver is not set.");
            return null;
        }
        if (this.poInvLedger == null) {
            this.poInvLedger = new Model_Inv_Ledger();
            this.poInvLedger.setApplicationDriver(this.poGRider);
            this.poInvLedger.setXML("Model_Inv_Ledger");
            this.poInvLedger.setTableName("Inv_Ledger");
            this.poInvLedger.initialize();
        }
        return this.poInvLedger;
    }

    public Model_Inv_Adjustment_Master InventoryAdjustmentMaster() {
        if (poGRider == null) {
            System.err.println("InvModels.InventoryAdjustmentMaster: Application driver is not set.");
            return null;
        }

        if (poInvAdjustmentMaster == null) {
            poInvAdjustmentMaster = new Model_Inv_Adjustment_Master();
            poInvAdjustmentMaster.setApplicationDriver(poGRider);
            poInvAdjustmentMaster.setXML("Model_Inv_Adjustment_Master");
            poInvAdjustmentMaster.setTableName("Inv_Adjustment_Master");
            poInvAdjustmentMaster.initialize();
        }

        return poInvAdjustmentMaster;
    }

    public Model_Inv_Adjustment_Detail InventoryAdjustmentDetail() {
        if (poGRider == null) {
            System.err.println("InvModels.InventoryAdjustmentDetail: Application driver is not set.");
            return null;
        }

        if (poInvAdjustmentDetail == null) {
            poInvAdjustmentDetail = new Model_Inv_Adjustment_Detail();
            poInvAdjustmentDetail.setApplicationDriver(poGRider);
            poInvAdjustmentDetail.setXML("Model_Inv_Adjustment_Detail");
            poInvAdjustmentDetail.setTableName("Inv_Adjustment_Detail");
            poInvAdjustmentDetail.initialize();
        }

        return poInvAdjustmentDetail;
    }

    public Model_Inv_Adjustment_Serial InventoryAdjustmentSerial() {
        if (poGRider == null) {
            System.err.println("InvModels.InventoryAdjustmentSerial: Application driver is not set.");
            return null;
        }

        if (poInvAdjustmentSerial == null) {
            poInvAdjustmentSerial = new Model_Inv_Adjustment_Serial();
            poInvAdjustmentSerial.setApplicationDriver(poGRider);
            poInvAdjustmentSerial.setXML("Model_Inv_Adjustment_Serial");
            poInvAdjustmentSerial.setTableName("Inv_Adjustment_Serial");
            poInvAdjustmentSerial.initialize();
        }

        return poInvAdjustmentSerial;
    }

    private final GRiderCAS poGRider;

    private Model_Inventory poInventory;
    private Model_InventorySuperseded poInventorySuperseded;
    private Model_Inv_Master poInvMaster;
    private Model_Inventory_Supplier poSupplier;
    private Model_Inv_Serial poInvSerial;
    private Model_Inv_Serial_Ledger poInvSerialLeger;
    private Model_Inv_Serial_Registration poInvSerialReg;
    private Model_Classification_Config poClassificationConfig;
    private Model_Inv_Ledger poInvLedger;
    private Model_Inventory_Child_Unit poInvChildUnit;
    private Model_Inv_Adjustment_Master poInvAdjustmentMaster;
    private Model_Inv_Adjustment_Detail poInvAdjustmentDetail;
    private Model_Inv_Adjustment_Serial poInvAdjustmentSerial;

    public Model_Inventory_Child_Unit Inventory_Child_Unit() {
        if (poGRider == null) {
            System.err.println("InvModels.Inventory_Child_Unit: Application driver is not set.");
            return null;
        }

        if (poInvChildUnit == null) {
            poInvChildUnit = new Model_Inventory_Child_Unit();
            poInvChildUnit.setApplicationDriver(poGRider);
            poInvChildUnit.setXML("Model_Inventory_Child_Unit");
            poInvChildUnit.setTableName("Inventory_Child_Unit");
            poInvChildUnit.initialize();
        }

        return poInvChildUnit;
    }
}
