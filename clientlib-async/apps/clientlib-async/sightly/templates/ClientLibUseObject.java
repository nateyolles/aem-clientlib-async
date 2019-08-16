package apps.clientlib_async.sightly.templates;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;

import javax.script.Bindings;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.api.resource.Resource;

import com.adobe.granite.ui.clientlibs.ClientLibrary;
import com.adobe.granite.ui.clientlibs.HtmlLibraryManager;
import com.adobe.granite.ui.clientlibs.LibraryType;

import org.apache.sling.xss.XSSAPI;

import org.slf4j.Logger;

import org.apache.sling.scripting.sightly.pojo.Use;

/**
 * Sightly Clientlibs that can accept expression options for 'defer', 'async'
 * 'onload' and 'crossorigin'.
 *
 * See: https://github.com/nateyolles/aem-clientlib-async
 *
 * This class is mostly code from /libs/granite/sightly/templates/ClientLibUseObjec.java,
 * found in your local AEM instance. The differences are that this class gets
 * the 'loading' and 'onload' attributes, gets the categories retrieved from
 * {@link com.day.cq.widget.HtmlLibraryManager#getLibraries(String[], LibraryType, boolean, boolean)}
 * and writes it's own HTML script elements rather than have the HtmlLibrary
 * manger do it for us using {@link com.day.cq.widget.HtmlLibraryManager#writeIncludes(SlingHttpServletRequest, Writer, String...)}.
 *
 * @author    Nate Yolles <yolles@adobe.com>
 * @version   2.0.0
 * @since     2015-03-19
 * @see       libs.granite.sightly.templates.ClientLibUseObject
 * @see       com.day.cq.widget.HtmlLibraryManager
 */
public class ClientLibUseObject implements Use {

    private static final String BINDINGS_CATEGORIES = "categories";
    private static final String BINDINGS_MODE = "mode";

    /**
     * Sightly parameter that becomes the script element void attribute such as
     * 'defer' and 'async'. Valid values are listed in {@link #VALID_JS_ATTRIBUTES}.
     */
    private static final String BINDINGS_LOADING = "loading";

    /**
     * Sightly parameter that becomes the javascript function value in the
     * script element's 'onload' attribute.
     */
    private static final String BINDINGS_ONLOAD = "onload";

    /**
     * Sightly parameter that becomes the value in the script and link elements'
     * 'crossorigin' attribute.
     */
    private static final String BINDINGS_CROSS_ORIGIN = "crossorigin";

    /**
     * HTML markup for javascript. Add 'type="text/javascript"' if you are not
     * using HTML 5.
     */
    private static final String TAG_JAVASCRIPT = "<script type=\"text/javascript\" src=\"%s\"%s></script>";

    /**
     * HTML markup for stylesheets.
     */
    private static final String TAG_STYLESHEET_PRELOAD	= "<link rel=\"preload\" href=\"%s\"%s as=\"style\">";
    private static final String TAG_STYLESHEET_INIT = "<link rel=\"stylesheet\" href=\"%s\"%s media=\"print\" type=\"text/css\" onload=\"this.media='all'\">";

    /**
     * HTML markup for onload attribute of script element.
     */
    private static final String ONLOAD_ATTRIBUTE = " onload=\"%s\"";

    /**
     * HTML markup for crossorigin attribute of script and link elements.
     */
    private static final String CROSS_ORIGIN_ATTRIBUTE = " crossorigin=\"%s\"";

    /**
     * Valid void attributes for HTML markup of script element.
     */
    private static final List<String> VALID_JS_ATTRIBUTES = new ArrayList<String>(){{
        add("async");
        add("defer");
    }};

    /**
     * Valid values for crossorigin attribute for HTML markup of script and link
     * elements.
     */
    private static final List<String> VALID_CROSS_ORIGIN_VALUES = new ArrayList<String>(){{
        add("anonymous");
        add("use-credentials");
    }};

    private HtmlLibraryManager htmlLibraryManager = null;
    private String[] categories;
    private String mode;
    private String loadingAttribute;
    private String onloadAttribute;
    private String crossoriginAttribute;
    private SlingHttpServletRequest request;
    private PrintWriter out;
    private Logger log;
    private Resource resource;
    private XSSAPI xssAPI;

    /**
     * Same as AEM provided method with the addition of getting the XSSAPI
     * service and the two additional bindings for loading and onload.
     * 
     * @see libs.granite.sightly.templates.ClientLibUseObject#init(Bindings)
     */
    public void init(Bindings bindings) {
        loadingAttribute = (String) bindings.get(BINDINGS_LOADING);
        onloadAttribute = (String) bindings.get(BINDINGS_ONLOAD);
        crossoriginAttribute = (String) bindings.get(BINDINGS_CROSS_ORIGIN);
        resource = (Resource) bindings.get("resource");

        Object categoriesObject = bindings.get(BINDINGS_CATEGORIES);
        log = (Logger) bindings.get(SlingBindings.LOG);
        if (categoriesObject != null) {
            if (categoriesObject instanceof Object[]) {
                Object[] categoriesArray = (Object[]) categoriesObject;
                categories = new String[categoriesArray.length];
                int i = 0;
                for (Object o : categoriesArray) {
                    if (o instanceof String) {
                        categories[i++] = ((String) o).trim();
                    }
                }
            } else if (categoriesObject instanceof String) {
                categories = ((String) categoriesObject).split(",");
                int i = 0;
                for (String c : categories) {
                    categories[i++] = c.trim();
                }
            }
            if (categories != null && categories.length > 0) {
                mode = (String) bindings.get(BINDINGS_MODE);
                request = (SlingHttpServletRequest) bindings.get(SlingBindings.REQUEST);
                SlingScriptHelper sling = (SlingScriptHelper) bindings.get(SlingBindings.SLING);
                htmlLibraryManager = sling.getService(HtmlLibraryManager.class);
                xssAPI = sling.getService(XSSAPI.class);
            }
        }
    }

    /**
     * Essentially the same as the AEM provided method with the exception that
     * the HtmlLibraryManger's writeIncludes methods have been replaced with
     * calls to #includeLibraries.
     * 
     * @see libs.granite.sightly.templates.ClientLibUseObject#include()
     */
    public String include() {
        StringWriter sw = new StringWriter();

        if (categories == null || categories.length == 0)  {
            log.error("'categories' option might be missing from the invocation of the /apps/beagle/sightly/templates/clientlib.html" +
                    "client libraries template library. Please provide a CSV list or an array of categories to include.");
        } else {
            PrintWriter out = new PrintWriter(sw);
            if ("js".equalsIgnoreCase(mode)) {
                includeLibraries(out, LibraryType.JS);
            } else if ("css".equalsIgnoreCase(mode)) {
                includeLibraries(out, LibraryType.CSS);
            } else {
                includeLibraries(out, LibraryType.CSS);
                includeLibraries(out, LibraryType.JS);
            }
        }

        return sw.toString();
    }

    /**
     * Construct the HTML markup for the script and link elements.
     *
     * @param out The PrintWriter object responsible for writing the HTML.
     * @param LibraryType The library type either CSS or JS.
     */
    private void includeLibraries(PrintWriter out, LibraryType libraryType) {
        if (htmlLibraryManager != null && libraryType != null && xssAPI != null) { 
            Collection<ClientLibrary> libs = htmlLibraryManager.getLibraries(categories, libraryType, false, false);

            String attribute = StringUtils.EMPTY;

            if (libraryType.equals(LibraryType.JS)) {
                if (StringUtils.isNotBlank(loadingAttribute) && VALID_JS_ATTRIBUTES.contains(loadingAttribute.toLowerCase())) {
                    attribute = " ".concat(loadingAttribute.toLowerCase());
                }

                if (StringUtils.isNotBlank(onloadAttribute)) {
                    String safeOnload = xssAPI.encodeForHTMLAttr(onloadAttribute);

                    if (StringUtils.isNotBlank(safeOnload)) {
                        attribute = attribute.concat(String.format(ONLOAD_ATTRIBUTE, safeOnload));
                    }
                }
            }

            if (StringUtils.isNotBlank(crossoriginAttribute) && VALID_CROSS_ORIGIN_VALUES.contains(crossoriginAttribute.toLowerCase())) {
                attribute = attribute.concat(String.format(CROSS_ORIGIN_ATTRIBUTE, crossoriginAttribute.toLowerCase()));
            }

            for (ClientLibrary lib : libs) {
                String path = getIncludePath(request, lib, libraryType, htmlLibraryManager.isMinifyEnabled());

                if (path != null) {
                    if (libraryType.equals(LibraryType.CSS)) {
                        out.format(TAG_STYLESHEET_PRELOAD, path, attribute);
                        out.format(TAG_STYLESHEET_INIT, path, attribute);
                    }
                    if (libraryType.equals(LibraryType.JS)) {
                        out.format(TAG_JAVASCRIPT, path, attribute);
                    }
                }
            }
        }
    }

    /**
     * Returns the include path for the given library and type, respecting the proxy settings.
     * @param lib library
     * @param type type
     * @param minify {@code true} for minify
     * @return the path
     *
     * @see com.adobe.granite.ui.clientlibs.impl.HtmlLibraryWriter#getIncludePath(SlingHttpServletRequest, ClientLibrary, LibraryType, boolean)
     */
    private String getIncludePath(SlingHttpServletRequest request, ClientLibrary lib, LibraryType type, boolean minify) {
        String path = lib.getIncludePath(type, minify);
        if (lib.allowProxy() && (path.startsWith("/libs/") || path.startsWith("/apps/"))) {
            path = "/etc.clientlibs" + path.substring(5);
        } else {
            // check if request session has access (GRANITE-4429)
            if (request.getResourceResolver().getResource(lib.getPath()) == null) {
                path = null;
            }
        }
        return path;
    }
}
