package challenge2;

import sootup.callgraph.CallGraph;
import sootup.callgraph.ClassHierarchyAnalysisAlgorithm;
import sootup.callgraph.RapidTypeAnalysisAlgorithm;
import sootup.core.inputlocation.AnalysisInputLocation;
import sootup.core.model.SootClass;
import sootup.core.model.SootMethod;
import sootup.core.model.SourceType;
import sootup.core.signatures.MethodSignature;
import sootup.core.types.ClassType;
import sootup.core.views.View;
import sootup.interceptors.LocalSplitter;
import sootup.interceptors.TypeAssigner;
import sootup.java.bytecode.frontend.inputlocation.JavaClassPathAnalysisInputLocation;
import sootup.java.core.JavaIdentifierFactory;
import sootup.java.core.views.JavaView;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Challenge2 {
    public static void main(String[] args) {
        // String pathToBinary = "src/test/resources/example/jcl-over-slf4j-2.0.7.jar";

        Path pathToJar = Paths.get(System.getProperty("user.dir") + "/boilerplate/src/test/resources/example/jcl-over-slf4j-2.0.7.jar");
        String sootCp = pathToJar.toString();
        AnalysisInputLocation inputLocation = new JavaClassPathAnalysisInputLocation(sootCp, SourceType.Application, List.of(new LocalSplitter(), new TypeAssigner()));
        View view = new JavaView(List.of(inputLocation));

//        JavaClassType classType = (JavaClassType) view.getIdentifierFactory().getClassType("Main");
//        MethodSignature methodSignature = view
//                        .getIdentifierFactory()
//                        .getMethodSignature(
//                                classType, "main", "void", Collections.singletonList("java.lang.String[]"));

        List<SootMethod> cgEntryPointMethods = getCGEntryPointMethods(view);
        List<MethodSignature> methodSignatures = cgEntryPointMethods.stream().map(m -> m.getSignature()).toList();

        ClassHierarchyAnalysisAlgorithm chaAlgorithm = new ClassHierarchyAnalysisAlgorithm(view);
        RapidTypeAnalysisAlgorithm rtaAlgorithm = new RapidTypeAnalysisAlgorithm(view);
//        CallGraph chaCG = chaAlgorithm.initialize(Collections.singletonList(methodSignature));
//        CallGraph rtaCG = rtaAlgorithm.initialize(Collections.singletonList(methodSignature));

        CallGraph chaCG = chaAlgorithm.initialize(methodSignatures);
        CallGraph rtaCG = rtaAlgorithm.initialize(methodSignatures);
        Set<MethodSignature> chaMethodSignatures = chaCG.getMethodSignatures();
        Set<MethodSignature> rtaMethodSignatures = rtaCG.getMethodSignatures();
        System.out.println("callcount: " + chaCG.callCount() + " methodsig size: " + chaCG.getMethodSignatures().size() + " cg entry methods size: " + chaCG.getEntryMethods().size() + " reachable methods size: " + chaCG.getReachableMethods().size());
        System.out.println("callcount: " + rtaCG.callCount() + " methodsig size: " + rtaCG.getMethodSignatures().size() + " cg entry methods size: " + rtaCG.getEntryMethods().size() + " reachable methods size: " + rtaCG.getReachableMethods().size());

        Set<MethodSignature> uniqueMethodSignatures = chaMethodSignatures;
        uniqueMethodSignatures.removeAll(rtaMethodSignatures);
        List<String> packagenames = new ArrayList<>();
        for (MethodSignature uniqueMethodSignature : uniqueMethodSignatures) {
            Set<CallGraph.Call> callingMethod = chaCG.callsTo(uniqueMethodSignature);
            for (CallGraph.Call signature : callingMethod) {
                ClassType callingMethodsignatureClass = signature.getSourceMethodSignature().getDeclClassType();
                String packageName = callingMethodsignatureClass.getPackageName().getName();
                packagenames.add(packageName);
            }
        }
        HashMap< String, Integer> countPackage  = new HashMap<>();
        for (String packagename : packagenames) {
            countPackage.putIfAbsent(packagename, 0);
            countPackage.put(packagename, countPackage.get(packagename) + 1);
        }
        System.out.println(countPackage);
        Set<String> packageList = countPackage.keySet();
        List<String> myList = new ArrayList<>(packageList);

        // Step 3: Sort the List
        Collections.sort(myList);
        System.out.println(countPackage);
        for (String element : myList) {
            System.out.println(countPackage.get(element));
        }
    }

    public static List<SootMethod> getCGEntryPointMethods(View view) {
        List<SootMethod> methods = new ArrayList<>();
        Set<SootClass> classes = new HashSet<>();
        classes.addAll(view.getClasses().toList());
        l1:
        for (SootClass c : classes) {
            for (SootMethod m : c.getMethods()) {
                JavaIdentifierFactory javaIdentifierFactory = JavaIdentifierFactory.getInstance();
                if (m.isMain(javaIdentifierFactory)) {
                    System.out.println("Main method found in a jar");
                }
                if (m.isConcrete()){
                    methods.add(m);
                }
            }
        }
        if (!methods.isEmpty()) {
            System.out.println(methods.size() + " methods will be used as entry points for cg");
            return methods;
        }
        System.out.println("no entry methods found to start");
        return Collections.EMPTY_LIST;
    }
}
