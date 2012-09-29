package org.tdar.struts.action;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.tdar.core.bean.resource.SensoryData;
import org.tdar.core.bean.resource.sensory.SensoryDataImage;
import org.tdar.core.bean.resource.sensory.SensoryDataScan;


public class SensoryDataControllerITCase extends AbstractAdminControllerITCase {

    private static String TEST_TITLE = "a title name";
    private static String TEST_DESC = "a description";
    private static Long INVALID_RESOURCE_ID = -1L;
    
    
    
    private SensoryDataController controller;

    @Override
    protected TdarActionSupport getController() {
        return null;
    }
    
    public void initControllerFields() {
        controller.prepare();
        controller.setResourceAvailability("public");
    }
    
    @Before
    public void prepController() {
        controller = generateNewInitializedController(SensoryDataController.class);
    }
    
    
    

    @Test
    public void testCreateBarebonesRecord() {
        initControllerFields();
        SensoryData resource = controller.getResource();
        Assert.assertNotNull("should have a blank, non-null resource", resource);
        
        resource.setTitle(TEST_TITLE);
        resource.setDescription(TEST_DESC);
        controller.save();
        Long id = resource.getId();
        Assert.assertNotSame("should have valid resource id", INVALID_RESOURCE_ID, id );
        
        
        //go back to the 'edit' page
        controller = generateNewInitializedController(SensoryDataController.class);
        controller.setResourceId(id);
        controller.prepare();
        Assert.assertSame("expecting same title", TEST_TITLE, controller.getResource().getTitle());
        Assert.assertSame("expecting same description", TEST_DESC, controller.getResource().getDescription());
    }
    
    
    
  @Test
  public void testSavingWithImageRecords() {
      initControllerFields();
      SensoryData resource = controller.getResource();
      Assert.assertSame(INVALID_RESOURCE_ID, resource.getId());
      Assert.assertNotNull("should have a blank, non-null resource", resource);
      resource.setTitle(TEST_TITLE);
      resource.setDescription(TEST_DESC);
      
      List<SensoryDataImage> images= new ArrayList<SensoryDataImage>();
      for(int i = 0; i < 10; i++) {
          SensoryDataImage image = new SensoryDataImage();
          image.setFilename("file" + i);
          image.setDescription("desc" + i);
          images.add(image);
          controller.getSensoryDataImages().add(image);
      }

      Long id = saveAndReload();
      Assert.assertEquals("expeciting  same nuber of image records", images.size(), resource.getSensoryDataImages().size());
      for(SensoryDataImage image : resource.getSensoryDataImages()) {
          Assert.assertNotNull("backpointer should be saved on image", image.getResource());
          logger.debug(image.getResource());
      }
      for(SensoryDataImage image : images) {
//          Assert.assertTrue("expecting image record in resource:" + image.getFilename(), resource.getSensoryDataImages().contains(image));
      }
  }
  
  
  @Test
  public void testSavingWithScanRecords() {
      initControllerFields();
      SensoryData resource = controller.getResource();
      Assert.assertNotNull("should have a blank, non-null resource", resource);
      resource.setTitle(TEST_TITLE);
      resource.setDescription(TEST_DESC);
      
      //TODO: grist for allentime mill: how to best check that a tranient object is 'similar' to to an object in my persisted list? 
      List<SensoryDataScan> scans= new ArrayList<SensoryDataScan>();
      for(int i = 0; i < 10; i++) {
          SensoryDataScan scan = new SensoryDataScan();
          scan.setFilename("file" + i);
          scan.setScanNotes("scan note" + i);
          scan.setPointsInScan(new Long((long)(Math.random()) * Long.MAX_VALUE));
          scans.add(scan);
          controller.getSensoryDataScans().add(scan);
      } 

      Long id = saveAndReload();
      Assert.assertEquals("should have same number of scans", 10, resource.getSensoryDataScans().size());
      for(SensoryDataScan scan: scans) {
          //TODO: grist for allentime mill: how to best check that a tranient object is 'similar' to to an object in my persisted list? 
//          Assert.assertTrue("expecting scan record in resource:" + scan.getFilename(), resource.getSensoryDataScans().contains(scan));
      }
  }
  
  
  
  
  
  
  private Long saveAndReload() {
      Long oldid = controller.getResource().getId();
      controller.save();
      Long id = controller.getResource().getId();
      controller = generateNewInitializedController(SensoryDataController.class);
      controller.setResourceId(id);
      controller.prepare();
      Assert.assertNotSame("resuorce should be assingned an id", oldid, id);
      return id;
  }    
    
        
}
