/*
 * Copyright (C) 2004 Technia AB
 * Knarrarnasgatan 13, SE-164 22, Kista, Sweden
 */

import matrix.db.Context;
import matrix.db.Environment;
import matrix.db.MQLCommand;

/**
 * Utility methods for TVC
 *
 * @version $Id: TVCUtils.java,v 1.1 2005/01/05 16:00:37 evi Exp $
 */
public class TVCUtils_mxJPO {
    /** Registration program */
    private static final String REG_PROG = 
        "eServiceSchemaVariableMapping.tcl";

    /**
     * Resolves a symbolic name.
     *
     * @param ctx           The context to use when 
     *                      resolving the symbolic 
     *                      name in the database.
     * @param symbolicName  The symbolic name to resolve.
     * @param defaultValue  The default value which is 
     *                      returned if the specified 
     *                      symbolic name isn't bound 
     *                      in the schema or if an 
     *                      unexpected error occurs.
     *
     * @return The resolved name, or the defaultValue if 
     *         the symbolic name isn't bound in the schema.
     */
    public static String resolveSymbolicName(Context ctx, 
        String symbolicName, String defaultValue) {
        try {
            return resolveSymbolicName(ctx, symbolicName);
        } catch(Exception e) {
            return defaultValue;
        }
    }

    /**
     * Resolves a symbolic name.
     *
     * @param ctx           The context to use when 
     *                      resolving the symbolic 
     *                      name in the database.
     * @param symbolicName  The symbolic name to resolve.
     *
     * @return The resolved name.
     *
     * @throws Exception If the name isn't bound in 
     * the schema or if an unexpected error occurs.
     */
    public static String resolveSymbolicName(Context ctx, 
        String symbolicName) throws Exception {

        MQLCommand mql = new MQLCommand();
        if (mql.executeCommand(ctx, "print program \"" + REG_PROG + "\" select property[" + symbolicName + "].to dump")) {
            String res = mql.getResult().trim();
            if (res == null || res.length() == 0) {
                throw new Exception(symbolicName + " not bound in schema");
            }
            int i = res.indexOf(' ');
            if (i == -1) {
                throw new Exception("Unable to parse output: " + mql.getResult());
            }
            return res.substring(i+1);
        } else {
            throw new Exception(mql.getError());
        }
    }
    
    public String getMCSUrl(Context ctx, String[] args) throws Exception {
        return Environment.getValue(ctx, "MX_MCS_URL");
    }
    
    public Boolean isFCSEnabled(Context ctx, String[] args) throws Exception {
        String s = Environment.getValue(ctx, "MX_FCS_ENABLED");
        return "false".equalsIgnoreCase(s) ? Boolean.FALSE : Boolean.TRUE;
    }
}
