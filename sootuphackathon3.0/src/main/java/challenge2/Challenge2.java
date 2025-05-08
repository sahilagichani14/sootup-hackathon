package challenge2;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import sootup.callgraph.CallGraph;
import sootup.callgraph.RapidTypeAnalysisAlgorithm;
import sootup.core.inputlocation.AnalysisInputLocation;
import sootup.core.jimple.common.expr.JStaticInvokeExpr;
import sootup.core.jimple.common.stmt.JInvokeStmt;
import sootup.core.model.SootClass;
import sootup.core.model.SootMethod;
import sootup.core.model.SourceType;
import sootup.core.signatures.MethodSignature;
import sootup.core.types.ClassType;
import sootup.java.bytecode.frontend.inputlocation.DefaultRuntimeAnalysisInputLocation;
import sootup.java.bytecode.frontend.inputlocation.JavaClassPathAnalysisInputLocation;
import sootup.java.core.JavaIdentifierFactory;
import sootup.java.core.JavaSootClass;
import sootup.java.core.views.JavaView;

public class Challenge2 {

  public static void main(String[] args) {
    List<AnalysisInputLocation> inputs=new ArrayList<>();
    inputs.add(new JavaClassPathAnalysisInputLocation("sootuphackathon3.0/src/main/java/challenge2/challenge2.jar", SourceType.Application));
    inputs.add(new DefaultRuntimeAnalysisInputLocation());

    JavaView view = new JavaView(inputs);
    ClassType classTypeA = view.getIdentifierFactory().getClassType("Main");
    SootClass c=view.getClass(classTypeA).orElse(null);

    MethodSignature entryMethodSignature =
        JavaIdentifierFactory.getInstance()
            .getMethodSignature(
                classTypeA,
                "main",
                "void",
                Collections.singletonList("java.lang.String[]"));
    SootMethod m= view.getMethod(entryMethodSignature).orElse(null);

    RapidTypeAnalysisAlgorithm rtaAlgorithm = new RapidTypeAnalysisAlgorithm(view);
    CallGraph rtaCG = rtaAlgorithm.initialize(Collections.singletonList(m.getSignature()));
    Set<MethodSignature> rtaMethodSignatures = rtaCG.getMethodSignatures();

    List<MethodSignature> collectedStealMoney = rtaMethodSignatures.stream().filter(methodSignature -> {
      return methodSignature.toString().contains("stealMoney");
    }).collect(Collectors.toList());
    MethodSignature collectedStealMoneySig = collectedStealMoney.get(0);
    System.out.println(collectedStealMoneySig);

    Set<CallGraph.Call> callsTo = rtaCG.callsTo(collectedStealMoneySig);

    int countPart1 = callsTo.size();
    System.out.println(countPart1);

    long countPart2 = callsTo.stream().flatMap(call -> rtaCG.callsTo(call.getSourceMethodSignature()).stream())
            .filter(call -> call.getInvokableStmt().getInvokeExpr().get() instanceof JStaticInvokeExpr).count();
    System.out.println(countPart2);

//    //approach 1
//    Set<MethodSignature> callSourcesToStealMoney = rtaCG.callSourcesTo(collectedStealMoneySig);
//    Set<MethodSignature> indirectCallers = callSourcesToStealMoney.stream()
//            .flatMap(midCaller -> rtaCG.callSourcesTo(midCaller).stream()) // a → b
//            .collect(Collectors.toSet());
//
//    indirectCallers.forEach(System.out::println);
//    Set<MethodSignature> pureIndirectCallers = indirectCallers.stream()
//            .filter(sig -> !callSourcesToStealMoney.contains(sig))
//            .collect(Collectors.toSet());
//
//    pureIndirectCallers.forEach(System.out::println);


//    // approach 2
//    Set<MethodSignature> callSourcesTo = rtaCG.callSourcesTo(collectedStealMoneySig);
//    System.out.println(callSourcesTo);
//
//    AtomicInteger countPart2 = new AtomicInteger();
//    callSourcesTo.forEach(callSrc -> {
//      Set<MethodSignature> callSourcesToSrcs = rtaCG.callSourcesTo(callSrc);
//      if (callSourcesToSrcs.size()>0){
//        countPart2.getAndIncrement();
//      }
//    });
//    System.out.println(countPart2);

    // is Path from myMoneyPile to stealMoney() exists
    List<MethodSignature> collectedMyMoneyPile = rtaMethodSignatures.stream().filter(methodSignature -> {
      return methodSignature.toString().contains("myMoneyPile");
    }).collect(Collectors.toList());
    MethodSignature MyMoneyPilemethodSignature = collectedMyMoneyPile.get(0);
    System.out.println(MyMoneyPilemethodSignature);

    boolean pathExists = false;
    Set<MethodSignature> visited = new HashSet<>();
    Deque<MethodSignature> worklist = new ArrayDeque<>(collectedMyMoneyPile);

    while (!worklist.isEmpty()) {
      MethodSignature current = worklist.poll();
      if (!visited.add(current)) continue; // skip if already visited

      if (current.equals(collectedStealMoneySig)) {
        pathExists = true;
        break;
      }

      Set<MethodSignature> callees = rtaCG.callTargetsFrom(current);
      worklist.addAll(callees);
    }

    if (pathExists) {
      System.out.println("path exists");
    }

    //Path from myData to leakData()
    List<MethodSignature> collectedMyData = rtaMethodSignatures.stream().filter(methodSignature -> {
      return methodSignature.toString().contains("myData");
    }).collect(Collectors.toList());
    MethodSignature MyDatamethodSignature = collectedMyData.get(0);

    List<MethodSignature> collectedleakData = rtaMethodSignatures.stream().filter(methodSignature -> {
      return methodSignature.toString().contains("leakData");
    }).collect(Collectors.toList());
    MethodSignature leakDatamethodSignature = collectedMyData.get(0);

    boolean pathExists1 = false;
    Set<MethodSignature> visited1 = new HashSet<>();
    Deque<MethodSignature> worklist1 = new ArrayDeque<>(collectedMyData);

    while (!worklist1.isEmpty()) {
      MethodSignature current = worklist1.poll();
      if (!visited1.add(current)) continue; // skip if already visited

      if (current.equals(leakDatamethodSignature)) {
        pathExists1 = true;
        break;
      }

      Set<MethodSignature> callees = rtaCG.callTargetsFrom(current);
      worklist1.addAll(callees);
    }

    if (pathExists1) {
      System.out.println("path exists 1");
    }

    // Step 1: Get all application class names (e.g., "b.Foo")
    List<String> allApplicationClasses = view.getClasses()
            .map(cls -> cls.getName())
            .filter(className -> className.startsWith("b.")) // limit to app package
            .collect(Collectors.toList());

  Set<String> reachableClassNames = rtaMethodSignatures.stream()
            .map(methodSignature -> methodSignature.getDeclClassType().toString())
            .filter(className -> className.contains("b."))
            .collect(Collectors.toSet());

    List<String> unreachableClasses = allApplicationClasses.stream()
            .filter(className -> !reachableClassNames.contains(className))
            .collect(Collectors.toList());
    System.out.println(unreachableClasses);
    // 23 56 0 1 c28i1i2i4

  }
}
