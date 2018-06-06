public class TVCTriggerManager_mxJPO {
    public TVCTriggerManager_mxJPO() {
    }

    public TVCTriggerManager_mxJPO(matrix.db.Context ctx, String[] args) {
    }

    public int mxMain(matrix.db.Context ctx, String[] args) throws Exception {
        return com.technia.tvc.core.db.domain.TriggerManager.perform(ctx, args);        
    }
}
