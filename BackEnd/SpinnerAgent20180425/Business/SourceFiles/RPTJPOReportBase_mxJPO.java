/*
 * Copyright (C) 2004 Technia AB
 * Knarrarnasgatan 13, SE-164 22, Kista, Sweden
 */

import matrix.db.*;
import matrix.util.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;

/*
 * DEPRECATED. Use Java based reports for better performance and control over
 * the report creation.
 */
public class RPTJPOReportBase_mxJPO {
    protected Context ctx;
    protected String reportDefOid;
    protected String reportDefType;
    protected String reportDefName;
    protected String reportDefRev;
    protected String reportOid;
    
    protected Map requestMap;
    protected Map convProps;
    protected OutputStream out;

    public RPTJPOReportBase_mxJPO(Context ctx, String[] args) {
        this.ctx = ctx;
        reportOid = args[0];
        reportDefOid = args[1];
        reportDefType = args[2];
        reportDefName = args[3];
        reportDefRev = args[4];
    }

    protected OutputStream getOut() {
        return out;
    }

    public final int generateXML(Context ctx, String[] args) throws Exception {
        Map paramMap = (Map) JPO.unpackArgs(args);
        String methodName = (String) paramMap.get("methodName");
        requestMap = (Map) paramMap.get("requestMap");
        convProps = (Map) paramMap.get("convProps");
        if (requestMap == null) {
            requestMap = convProps;
        }
        Object outputStream = paramMap.get("outputStream");

        /*
         * Use reflection to avoid compilation error
         * when TVC classes are not available.
         */
        Class remoteOSClass = Class.forName("com.technia.tvc.reportgenerator.remote.rmi.RemoteOutputStream");
        Class proxyOSClass = Class.forName("com.technia.tvc.reportgenerator.remote.rmi.ProxyOutputStream");
        Constructor constructor;
        Class[] paramTypes = new Class[] { remoteOSClass };
        constructor = proxyOSClass.getConstructor(paramTypes);
        Object proxyOS = constructor.newInstance(new Object[] { outputStream });
        
        /*
         * Create the output stream being used.
         */
        out = new BufferedOutputStream((OutputStream) proxyOS);

        Class thisClass = this.getClass();
        Method method = thisClass.getDeclaredMethod(methodName, new Class[0]);
        method.invoke(this, new Object[0]);

        return 0;
    }
}
