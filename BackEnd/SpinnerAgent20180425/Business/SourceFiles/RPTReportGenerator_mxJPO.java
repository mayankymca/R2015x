/*
 * Copyright (C) 2004 Technia AB
 * Knarrarnasgatan 13, SE-164 22, Kista, Sweden
 */
import matrix.db.Context;
import com.technia.tvc.reportgenerator.Client;

/**
 * <p>JPO Wrapper for the report generator.</p>
 */
public class RPTReportGenerator_mxJPO {
    public RPTReportGenerator_mxJPO(Context ctx, String[] args) {
    }

    public int mxMain(Context ctx, String[] args) throws Exception {
        return Client.triggerMain(ctx, args);
    }
}
