package io.github.vipcxj.jasync.core.jdt;

import io.github.vipcxj.jasync.core.Logger;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.*;
import org.eclipse.jdt.apt.core.internal.AptPlugin;
import org.eclipse.jdt.apt.core.util.AptPreferenceConstants;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.osgi.service.prefs.BackingStoreException;

import java.io.File;
import java.util.*;
import java.util.regex.Pattern;

@SuppressWarnings({"CollectionAddAllCanBeReplacedWithConstructor", "UnusedAssignment", "RedundantCast", "unused"})
public class AptConfig {

    /** regex to identify substituted token in path variables */
    private static final String PATHVAR_TOKEN = "^%[^%/\\\\ ]+%.*"; //$NON-NLS-1$
    /** path variable meaning "workspace root" */
    private static final String PATHVAR_ROOT = "%ROOT%"; //$NON-NLS-1$
    /** path variable meaning "project root" */
    private static final String PATHVAR_PROJECTROOT = "%PROJECT.DIR%"; //$NON-NLS-1$

    /**
     * Get the options that are presented to annotation processors by the
     * AnnotationProcessorEnvironment.  Options are key/value pairs which
     * are set in the project properties.
     *
     * Option values can begin with a percent-delimited token representing
     * a classpath variable or one of several predefined values.  The token
     * must either be followed by a path delimiter, or be the entire value.
     * Such tokens will be replaced with their resolved value.  The predefined
     * values are <code>%ROOT%</code>, which is replaced by the absolute pathname
     * of the workspace root directory, and <code>%PROJECT.DIR%</code>, which
     * will be replaced by the absolute pathname of the project root directory.
     * For example, a value of <code>%ECLIPSE_HOME%/configuration/config.ini</code>
     * might be resolved to <code>d:/eclipse/configuration/config.ini</code>.
     *
     * This method returns some options which are set programmatically but
     * are not directly editable, are not displayed in the configuration GUI,
     * and are not persisted to the preference store.  This is meant to
     * emulate the behavior of Sun's apt command-line tool, which passes
     * most of its command line options to the processor environment.  The
     * programmatically set options are:
     *  <code>-classpath</code> [set to Java build path]
     *  <code>-sourcepath</code> [set to Java source path]
     *  <code>-s</code> [set to generated src dir]
     *  <code>-d</code> [set to binary output dir]
     *  <code>-target</code> [set to compiler target version]
     *  <code>-source</code> [set to compiler source version]
     *
     * There are some slight differences between the options returned by this
     * method and the options returned from this implementation of @see
     * AnnotationProcessorEnvironment#getOptions().  First, that method returns
     * additional options which are only meaningful during a build, such as
     * <code>phase</code>.  Second, that method also adds alternate encodings
     * of each option, to be compatible with a bug in Sun's apt implementation:
     * specifically, for each option key="k", value="v", an additional option
     * is created with key="-Ak=v", value=null.  This includes the user-created
     * options, but does not include the programmatically defined options listed
     * above.
     *
     * @param jproj a project, or null to query the workspace-wide setting.
     * @return a mutable, possibly empty, map of (key, value) pairs.
     * The value part of a pair may be null (equivalent to "-Akey" on the Sun apt
     * command line).
     * The value part may contain spaces.
     * @since 3.6
     */
    public static Map<String, String> getProcessorOptions(IJavaProject jproj) {
        Map<String,String> rawOptions = getRawProcessorOptions(jproj);
        // map is large enough to also include the programmatically generated options
        Map<String, String> options = new HashMap<>(rawOptions.size() + 6);

        // Resolve path metavariables like %ROOT%
        for (Map.Entry<String, String> entry : rawOptions.entrySet()) {
            String resolvedValue = resolveVarPath(jproj, entry.getValue());
            String value = (resolvedValue == null) ? entry.getValue() : resolvedValue;
            options.put(entry.getKey(), value);
        }

        if (jproj == null) {
            // there are no programmatically set options at the workspace level
            return options;
        }

        IWorkspaceRoot root = jproj.getProject().getWorkspace().getRoot();

        // Add sourcepath and classpath variables
        try {
            IClasspathEntry[] classpathEntries = jproj.getResolvedClasspath(true);
            Set<String> classpath = new LinkedHashSet<>();
            Set<String> sourcepath = new LinkedHashSet<>();

            // For projects on the classpath, loops can exist; need to make sure we
            // don't loop forever
            Set<IJavaProject> projectsProcessed = new HashSet<>();
            projectsProcessed.add(jproj);
            for (IClasspathEntry entry : classpathEntries) {
                if (entry.isTest()) {
                    continue;
                }
                int kind = entry.getEntryKind();
                if (kind == IClasspathEntry.CPE_LIBRARY) {
                    IPath cpPath = entry.getPath();

                    IResource res = root.findMember(cpPath);

                    // If res is null, the path is absolute (it's an external jar)
                    if (res == null) {
                        classpath.add(cpPath.toOSString());
                    }
                    else {
                        // It's relative
                        classpath.add(res.getLocation().toOSString());
                    }
                }
                else if (kind == IClasspathEntry.CPE_SOURCE) {
                    IResource res = root.findMember(entry.getPath());
                    if (res == null) {
                        continue;
                    }
                    IPath srcPath = res.getLocation();
                    if (srcPath == null) {
                        continue;
                    }

                    sourcepath.add(srcPath.toOSString());
                }
                else if (kind == IClasspathEntry.CPE_PROJECT) {
                    // Add the dependent project's build path and classpath to ours
                    IPath otherProjectPath = entry.getPath();
                    IProject otherProject = root.getProject(otherProjectPath.segment(0));

                    // Note: JavaCore.create() is safe, even if the project is null --
                    // in that case, we get null back
                    IJavaProject otherJavaProject = JavaCore.create(otherProject);

                    // If it doesn't exist, ignore it
                    if (otherJavaProject != null && otherJavaProject.getProject().isOpen()) {
                        addProjectClasspath(root, otherJavaProject, projectsProcessed, classpath, false);
                    }
                }
            }
            // if you add options here, also add them in isAutomaticProcessorOption(),
            // and document them in docs/reference/automatic_processor_options.html.

            // Classpath and sourcepath
            options.put("-classpath",convertPathCollectionToString(classpath)); //$NON-NLS-1$
            options.put("-sourcepath", convertPathCollectionToString(sourcepath)); //$NON-NLS-1$

            // Get absolute path for generated source dir
            IFolder genSrcDir = jproj.getProject().getFolder(getGenSrcDir(jproj));
            String genSrcDirString = genSrcDir.getRawLocation().toOSString();
            options.put("-s", genSrcDirString); //$NON-NLS-1$

            // Absolute path for bin dir as well
            IPath binPath = jproj.getOutputLocation();
            IResource binPathResource = root.findMember(binPath);
            String binDirString;
            if (binPathResource != null) {
                binDirString = root.findMember(binPath).getLocation().toOSString();
            }
            else {
                binDirString = binPath.toOSString();
            }
            options.put("-d", binDirString); //$NON-NLS-1$

            String target = jproj.getOption(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, true);
            options.put("-target", target); //$NON-NLS-1$

            String source = jproj.getOption(JavaCore.COMPILER_SOURCE, true);
            options.put("-source", source); //$NON-NLS-1$
        }
        catch (JavaModelException jme) {
            Logger.error("Could not get the classpath for project: " + jproj); //$NON-NLS-1$
            Logger.error(jme);
        }

        return options;
    }

    public static String getGenSrcDir(IJavaProject jproject) {
        return getString(jproject, AptPreferenceConstants.APT_GENSRCDIR);
    }

    public static String getGenTestSrcDir(IJavaProject jproject) {
        return getString(jproject, AptPreferenceConstants.APT_GENTESTSRCDIR);
    }

    private static String convertPathCollectionToString(Collection<String> paths) {
        if (paths.size() == 0) {
            return ""; //$NON-NLS-1$
        }
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String path : paths) {
            if (first) {
                first = false;
            }
            else {
                sb.append(File.pathSeparatorChar);
            }
            sb.append(path);
        }
        return sb.toString();
    }

    // We need this as a separate method, as we'll put dependent projects' output
    // on the classpath
    private static void addProjectClasspath(
            IWorkspaceRoot root,
            IJavaProject otherJavaProject,
            Set<IJavaProject> projectsProcessed,
            Set<String> classpath,
            boolean isTestCode) {

        // Check for cycles. If we've already seen this project,
        // no need to go any further.
        if (projectsProcessed.contains(otherJavaProject)) {
            return;
        }
        projectsProcessed.add(otherJavaProject);

        try {
            // Add the output directory first as a binary entry for other projects
            IPath binPath = otherJavaProject.getOutputLocation();
            IResource binPathResource = root.findMember(binPath);
            String binDirString;
            if (binPathResource != null) {
                binDirString = root.findMember(binPath).getLocation().toOSString();
            }
            else {
                binDirString = binPath.toOSString();
            }
            classpath.add(binDirString);

            // Now the rest of the classpath
            IClasspathEntry[] classpathEntries = otherJavaProject.getResolvedClasspath(true);
            for (IClasspathEntry entry : classpathEntries) {
                if (!isTestCode && entry.isTest()) {
                    continue;
                }
                if (entry.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
                    IPath cpPath = entry.getPath();

                    IResource res = root.findMember(cpPath);

                    // If res is null, the path is absolute (it's an external jar)
                    if (res == null) {
                        classpath.add(cpPath.toOSString());
                    }
                    else {
                        // It's relative
                        classpath.add(res.getLocation().toOSString());
                    }
                }
                else if (entry.getEntryKind() == IClasspathEntry.CPE_PROJECT) {
                    IPath otherProjectPath = entry.getPath();
                    IProject otherProject = root.getProject(otherProjectPath.segment(0));
                    IJavaProject yetAnotherJavaProject = JavaCore.create(otherProject);
                    if (yetAnotherJavaProject != null) {
                        addProjectClasspath(root, yetAnotherJavaProject, projectsProcessed, classpath, isTestCode);
                    }
                }
                // Ignore source types
            }
        }
        catch (JavaModelException jme) {
            Logger.error("Failed to get the classpath for the following project: " + otherJavaProject);
            Logger.error(jme); //$NON-NLS-1$
        }
    }

    /**
     * If the value starts with a path variable such as %ROOT%, replace it with
     * the absolute path.
     * @param value the value of a -Akey=value command option
     */
    private static String resolveVarPath(IJavaProject jproj, String value) {
        if (value == null) {
            return null;
        }
        // is there a token to substitute?
        if (!Pattern.matches(PATHVAR_TOKEN, value)) {
            return value;
        }
        IPath path = new Path(value);
        String firstToken = path.segment(0);
        // If it matches %ROOT%/project, it is a project-relative path.
        if (PATHVAR_ROOT.equals(firstToken)) {
            IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
            IResource proj = root.findMember(path.segment(1));
            if (proj == null) {
                return value;
            }
            // all is well; do the substitution
            IPath relativePath = path.removeFirstSegments(2);
            IPath absoluteProjPath = proj.getLocation();
            IPath absoluteResPath = absoluteProjPath.append(relativePath);
            return absoluteResPath.toOSString();
        }

        // If it matches %PROJECT.DIR%/project, the path is relative to the current project.
        if (jproj != null && PATHVAR_PROJECTROOT.equals(firstToken)) {
            // all is well; do the substitution
            IPath relativePath = path.removeFirstSegments(1);
            IPath absoluteProjPath = jproj.getProject().getLocation();
            IPath absoluteResPath = absoluteProjPath.append(relativePath);
            return absoluteResPath.toOSString();
        }

        // otherwise it's a classpath-var-based path.
        String cpvName = firstToken.substring(1, firstToken.length() - 1);
        IPath cpvPath = JavaCore.getClasspathVariable(cpvName);
        if (cpvPath != null) {
            IPath resolved = cpvPath.append(path.removeFirstSegments(1));
            return resolved.toOSString();
        }
        else {
            return value;
        }
    }

    /**
     * Get the options that are presented to annotation processors by the
     * AnnotationProcessorEnvironment.  The -A and = are stripped out, so
     * (key, value) is the equivalent of -Akey=value.
     *
     * This method differs from getProcessorOptions in that the options returned
     * by this method do NOT include any programmatically set options.  This
     * method returns only the options that are persisted to the preference
     * store and that are displayed in the configuration GUI.
     *
     * @param jproj a project, or null to query the workspace-wide setting.
     * If jproj is not null, but the project has no per-project settings,
     * this method will fall back to the workspace-wide settings.
     * @return a mutable, possibly empty, map of (key, value) pairs.
     * The value part of a pair may be null (equivalent to "-Akey").
     * The value part can contain spaces, if it is quoted: -Afoo="bar baz".
     */
    public static Map<String, String> getRawProcessorOptions(IJavaProject jproj) {
        Map<String, String> options = new HashMap<>();

        // TODO: this code is needed only for backwards compatibility with
        // settings files previous to 2005.11.13.  At some point it should be
        // removed.
        // If an old-style setting exists, add it into the mix for backward
        // compatibility.
        options.putAll(getOldStyleRawProcessorOptions(jproj));

        // Fall back from project to workspace scope on an all-or-nothing basis,
        // not value by value.  (Never fall back to default scope; there are no
        // default processor options.)  We can't use IPreferencesService for this
        // as we would normally do, because we don't know the names of the keys.
        IScopeContext[] contexts;
        if (jproj != null) {
            contexts = new IScopeContext[] {
                    new ProjectScope(jproj.getProject()), InstanceScope.INSTANCE };
        }
        else {
            contexts = new IScopeContext[] { InstanceScope.INSTANCE };
        }
        for (IScopeContext context : contexts) {
            IEclipsePreferences prefs = context.getNode(AptPlugin.PLUGIN_ID);
            try {
                if (prefs.childrenNames().length > 0) {
                    IEclipsePreferences procOptionsNode = context.getNode(
                            AptPlugin.PLUGIN_ID + "/" + AptPreferenceConstants.APT_PROCESSOROPTIONS); //$NON-NLS-1$
                    if (procOptionsNode != null) {
                        for (String key : procOptionsNode.keys()) {
                            String nonNullVal = procOptionsNode.get(key, null);
                            String val = AptPreferenceConstants.APT_NULLVALUE.equals(nonNullVal) ?
                                    null : nonNullVal;
                            options.put(key, val);
                        }
                        break;
                    }
                }
            } catch (BackingStoreException e) {
                Logger.error("Unable to load annotation processor options");
                Logger.error(e);
            }
        }
        return options;
    }

    /**
     * TODO: this code is needed only for backwards compatibility with
     * settings files previous to 2005.11.13.  At some point it should be
     * removed.
     * Get the processor options as an APT-style string ("-Afoo=bar -Abaz=quux")
     */
    private static Map<String, String> getOldStyleRawProcessorOptions(IJavaProject jproj) {
        Map<String, String> options;
        String allOptions = getString(jproj, AptPreferenceConstants.APT_PROCESSOROPTIONS);
        if (null == allOptions) {
            options = new HashMap<>();
        }
        else {
            ProcessorOptionsParser op = new ProcessorOptionsParser(allOptions);
            options = op.parse();
        }
        return options;
    }

    public static String getString(IJavaProject jproj, String optionName) {
        IPreferencesService service = Platform.getPreferencesService();
        IScopeContext[] contexts;
        if (jproj != null) {
            contexts = new IScopeContext[]{new ProjectScope(jproj.getProject()), InstanceScope.INSTANCE, DefaultScope.INSTANCE};
        } else {
            contexts = new IScopeContext[]{InstanceScope.INSTANCE, DefaultScope.INSTANCE};
        }

        String pluginId = null;
        if ("org.eclipse.jdt.core.compiler.processAnnotations".equals(optionName)) {
            pluginId = "org.eclipse.jdt.core";
        } else {
            pluginId = "org.eclipse.jdt.apt.core";
        }

        return service.getString(pluginId, optionName, (String) AptPreferenceConstants.DEFAULT_OPTIONS_MAP.get(optionName), contexts);
    }

    /**
     * TODO: this code is needed only for backwards compatibility with
     * settings files previous to 2005.11.13.  At some point it should be
     * removed.
     *
     * Used to parse an apt-style command line string into a map of key/value
     * pairs.
     * Parsing ignores errors and simply tries to gobble up as many well-formed
     * pairs as it can find.
     */
    private static class ProcessorOptionsParser {
        final String _s;
        int _start; // everything before this is already parsed.
        boolean _hasVal; // does the last key found have a value token?

        public ProcessorOptionsParser(String s) {
            _s = s;
            _start = 0;
            _hasVal = false;
        }

        public Map<String, String> parse() {
            Map<String, String> options = new HashMap<>();
            String key;
            while (null != (key = parseKey())) {
                options.put(key, parseVal());
            }
            return options;
        }

        /**
         * Skip until a well-formed key (-Akey[=val]) is found, and
         * return the key.  Set _start to the beginning of the value,
         * or to the first character after the end of the key and
         * delimiter, for a valueless key.  Set _hasVal according to
         * whether a value was found.
         * @return a key, or null if no well-formed keys can be found.
         */
        private String parseKey() {
            String key;
            int spaceAt = -1;
            int equalsAt = -1;

            _hasVal = false;

            while (true) {
                _start = _s.indexOf("-A", _start); //$NON-NLS-1$
                if (_start < 0) {
                    return null;
                }

                // we found a -A.  The key is everything up to the next '=' or ' ' or EOL.
                _start += 2;
                if (_start >= _s.length()) {
                    // it was just a -A, nothing following.
                    return null;
                }

                spaceAt = _s.indexOf(' ', _start);
                equalsAt = _s.indexOf('=', _start);
                if (spaceAt == _start || equalsAt == _start) {
                    // false alarm.  Keep trying.
                    ++_start;
                    continue;
                }
                break;
            }

            // We found a legitimate -A with some text after it.
            // Where does the key end?
            if (equalsAt > 0) {
                if (spaceAt < 0 || equalsAt < spaceAt) {
                    // there is an equals, so there is a value.
                    key = _s.substring(_start, equalsAt);
                    _start = equalsAt + 1;
                    _hasVal = (_start < _s.length());
                }
                else {
                    // the next thing is a space, so this is a valueless key
                    key = _s.substring(_start, spaceAt);
                    _start = spaceAt + 1;
                }
            }
            else {
                if (spaceAt < 0) {
                    // no equals sign and no spaces: a valueless key, up to the end of the string.
                    key = _s.substring(_start);
                    _start = _s.length();
                }
                else {
                    // the next thing is a space, so this is a valueless key
                    key = _s.substring(_start, spaceAt);
                    _start = spaceAt + 1;
                }
            }
            return key;
        }

        /**
         * A value token is delimited by a space; but spaces inside quoted
         * regions are ignored.  A value may include multiple quoted regions.
         * An unmatched quote is treated as if there was a matching quote at
         * the end of the string.  Quotes are returned as part of the value.
         * @return the value, up to the next nonquoted space or end of string.
         */
        private String parseVal() {
            if (!_hasVal || _start < 0 || _start >= _s.length()) {
                return null;
            }
            boolean inQuotedRegion = false;
            int start = _start;
            int end = _start;
            while (end < _s.length()) {
                char c = _s.charAt(end);
                if (c == '"') {
                    inQuotedRegion = !inQuotedRegion;
                }
                else if (!inQuotedRegion && c == ' ') {
                    // end of token.
                    _start = end + 1;
                    break;
                }
                ++end;
            }

            return _s.substring(start, end);
        }
    }
}
