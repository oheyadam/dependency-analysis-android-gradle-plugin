package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Plugin
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.moduleCoordinates
import static com.autonomousapps.AdviceHelper.projectAdviceForDependencies
import static com.autonomousapps.kit.Dependency.jakartaInject
import static com.autonomousapps.kit.Dependency.slf4j

final class JavaModulesProject extends AbstractProject {

  final GradleProject gradleProject
  final boolean declareAsApi

  JavaModulesProject(boolean declareAsApi) {
    this.declareAsApi = declareAsApi
    this.gradleProject = build()
  }

  private GradleProject build() {
    def builder = newGradleProjectBuilder()
    builder.withSubproject('proj') { s ->
      s.sources = sources
      s.withBuildScript { bs ->
        bs.plugins = [Plugin.javaLibraryPlugin]
        bs.dependencies = [
          // should always be implementation as package 'com.example.internal' is not exported
          slf4j(declareAsApi? 'api' : 'implementation'),
          jakartaInject('api')
        ]
      }
    }

    def project = builder.build()
    project.writer().write()
    return project
  }

  private sources = [
    // Adding a 'module-info' file automatically activates Java Module compilation (uses --module-path)
    new Source(
      SourceType.JAVA, "module-info", "",
      """\
        module org.example {
          requires jakarta.inject;
          requires org.slf4j;
          
          exports com.example;
        }
      """.stripIndent()
    ),
    new Source(
      SourceType.JAVA, "Example", "com/example",
      """\
        package com.example;
        
        import jakarta.inject.Inject;
        import org.slf4j.helpers.NOPLogger;
        import com.example.internal.ExampleInternal;
        
        public class Example {
          @Inject
          public Example() {
            new ExampleInternal(NOPLogger.NOP_LOGGER);
          }
        }
      """.stripIndent()
    ),
    new Source(
      SourceType.JAVA, "ExampleInternal", "com/example/internal",
      """\
        package com.example.internal;

        import org.slf4j.Logger;
        
        public class ExampleInternal {
          public ExampleInternal(Logger sharedLogger) { }
        }
      """.stripIndent()
    )
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  final Set<ProjectAdvice> expectedBuildHealthImplementation = [
    projectAdviceForDependencies(':proj', [] as Set<Advice>)
  ]

  final Set<ProjectAdvice> expectedBuildHealthApi = [
    projectAdviceForDependencies(':proj',
      [Advice.ofChange(moduleCoordinates(slf4j('api')), 'api', 'implementation')] as Set<Advice>
    )
  ]
}
