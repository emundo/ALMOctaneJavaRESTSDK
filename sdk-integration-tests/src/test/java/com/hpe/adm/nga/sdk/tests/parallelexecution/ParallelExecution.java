package com.hpe.adm.nga.sdk.tests.parallelexecution;

import com.hpe.adm.nga.sdk.EntityList;
import com.hpe.adm.nga.sdk.NGA;
import com.hpe.adm.nga.sdk.authorisation.Authorisation;
import com.hpe.adm.nga.sdk.authorisation.UserAuthorisation;
import com.hpe.adm.nga.sdk.model.EntityModel;
import com.hpe.adm.nga.sdk.tests.base.TestBase;
import com.hpe.adm.nga.sdk.utils.AuthorisationUtils;
import com.hpe.adm.nga.sdk.utils.CommonUtils;
import com.hpe.adm.nga.sdk.utils.ConfigurationUtils;
import com.hpe.adm.nga.sdk.utils.ContextUtils;
import com.hpe.adm.nga.sdk.utils.generator.DataGenerator;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Collection;

/**
 * Created by Dmitry Zavyalov on 09/05/2016.
 */

@Ignore //Before remove ignore, please support username = "rest2@hpe.com" with password = "Welcome2"
public class ParallelExecution extends TestBase {

    @Test
    public void testParallelExecution_two_clients() throws Exception {
        String entityName1 = "product_areas";
        String entityName2 = "defects";

        NGA nga1 = getNgaClientFirst();
        EntityList entityList1 = nga1.entityList(entityName1);

        NGA nga2 = getNgaClientSecond();
        EntityList entityList2 = nga2.entityList(entityName2);

        Collection<EntityModel> generatedEntity1 = DataGenerator.generateEntityModel(nga1, entityName1);
        Collection<EntityModel> entityModels1 = entityList1.create().entities(generatedEntity1).execute();
        EntityModel entityModel1 = entityModels1.iterator().next();
        int entityId1 = CommonUtils.getIdFromEntityModel(entityModel1);

        Collection<EntityModel> generatedEntity2 = DataGenerator.generateEntityModel(nga2, entityName2);
        Collection<EntityModel> entityModels2 = entityList2.create().entities(generatedEntity2).execute();
        EntityModel entityModel2 = entityModels2.iterator().next();
        int entityId2 = CommonUtils.getIdFromEntityModel(entityModel2);

        int counter = 0;
        do {
            EntityModel getEntity1 = entityList1.at(entityId1).get().execute();
            Assert.assertTrue(CommonUtils.isEntityAInEntityB(generatedEntity1.iterator().next(), getEntity1));
            sleepTime(5);
            EntityModel getEntity2 = entityList2.at(entityId2).get().execute();
            Assert.assertTrue(CommonUtils.isEntityAInEntityB(generatedEntity2.iterator().next(), getEntity2));
            sleepTime(5);
            counter++;
        } while (counter < 5);
    }

    private NGA getNgaClientFirst() {
        final ConfigurationUtils configuration = ConfigurationUtils.getInstance();
        String url = configuration.getString("sdk.url");
        Authorisation authorisation = AuthorisationUtils.getAuthorisation();
        String sharedSpaceId = configuration.getString("sdk.sharedSpaceId");
        String workspaceId = configuration.getString("sdk.workspaceId");

        nga = ContextUtils.getContextWorkspace(url, authorisation, sharedSpaceId, workspaceId);
        return nga;
    }

    private NGA getNgaClientSecond() {
        final ConfigurationUtils configuration = ConfigurationUtils.getInstance();
        String url = configuration.getString("sdk.url");
        Authorisation authorisation = new UserAuthorisation("rest2@hpe.com", "Welcome2");
        String sharedSpaceId = "2002";
        String workspaceId = configuration.getString("sdk.workspaceId");

        nga = ContextUtils.getContextWorkspace(url, authorisation, sharedSpaceId, workspaceId);
        return nga;
    }

    private void sleepTime(int sleepTimeInSec) {
        try {
            Thread.sleep(sleepTimeInSec * 1000);
        } catch (Exception e) {
            System.out.println("Sleep exception...");
        }
    }
}
