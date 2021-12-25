package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.RippleProject
import org.spockframework.runtime.extension.builtin.PreconditionContext
import spock.lang.IgnoreIf

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

// TODO V2: Uncertain if we want to keep this feature in v2
@IgnoreIf({ PreconditionContext it -> it.sys.v == '2' })
final class RippleSpec extends AbstractJvmSpec {

  def "ripples account for transitive dependencies (#gradleVersion)"() {
    given:
    def project = new RippleProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, ':ripples', '--id', ':b')

    then:
    assertThat(actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)
    assertThat(actualRipples().ripples).containsExactlyElementsIn(project.expectedRipplesFromB)

    where:
    gradleVersion << gradleVersions()
  }
}
