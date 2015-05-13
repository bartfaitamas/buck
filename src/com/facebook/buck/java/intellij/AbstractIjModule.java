/*
 * Copyright 2015-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.facebook.buck.java.intellij;

import com.facebook.buck.model.BuildTarget;
import com.facebook.buck.rules.TargetNode;
import com.facebook.buck.util.BuckConstant;
import com.facebook.buck.util.immutables.BuckStyleImmutable;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import org.immutables.value.Value;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Represents a single IntelliJ module.
 */
@Value.Immutable
@BuckStyleImmutable
abstract class AbstractIjModule implements IjProjectElement {

  /**
   * This directory is analogous to the gen/ directory that IntelliJ would produce when building an
   * Android module. It contains files such as R.java, BuildConfig.java, and Manifest.java.
   * <p>
   * By default, IntelliJ generates its gen/ directories in our source tree, which would likely
   * mess with the user's use of {@code glob(['**&#x2f;*.java'])}.
   */
  private static final Path ANDROID_GEN_PATH = BuckConstant.BUCK_OUTPUT_PATH.resolve("android");

  @Override
  @Value.Derived
  public String getName() {
    return Util.intelliJModuleNameFromPath(getModuleBasePath());
  }

  @Override
  public abstract ImmutableSet<TargetNode<?>> getTargets();

  /**
   * @return path to the top-most directory the module is responsible for. This is also where the
   * corresponding .iml file is located.
   */
  public abstract Path getModuleBasePath();

  /**
   * @return paths to various directories the module is responsible for.
   */
  public abstract ImmutableSet<IjFolder> getFolders();

  /**
   * @return map of {@link BuildTarget}s the module depends on and information on whether it's a
   *         test-only dependency or not.
   */
  public abstract ImmutableMap<BuildTarget, IjModuleGraph.DependencyType> getDependencies();

  public abstract Optional<IjModuleAndroidFacet> getAndroidFacet();

  /**
   * @return path where the XML describing the module to IntelliJ will be written to.
   */
  @Value.Derived
  public Path getModuleImlFilePath() {
    return getModuleBasePath().resolve(getName() + ".iml");
  }

  /**
   * @return the relative path of gen from the base path of current module.  For example, for the
   * build target in $PROJECT_DIR$/android_res/com/facebook/gifts/, this will return
   * ../../../../buck-out/android/android_res/com/facebook/gifts/gen.
   */
  @Value.Derived
  public Path getRelativeGenPath() {
    Path pathBackUpToProjectRoot = getModuleBasePath().relativize(Paths.get(""));
    return pathBackUpToProjectRoot
        .resolve(ANDROID_GEN_PATH)
        .resolve(getModuleBasePath())
        .resolve("gen");
  }

  @Value.Check
  protected void targetSetCantBeEmpty() {
    Preconditions.checkArgument(!getTargets().isEmpty());
  }

  @Value.Check
  protected void allRulesAreChildrenOfBasePath() {
    Path moduleBasePath = getModuleBasePath();
    for (TargetNode<?> target : getTargets()) {
      Path targetBasePath = target.getBuildTarget().getBasePath();
      Preconditions.checkArgument(
          targetBasePath.startsWith(moduleBasePath),
          "A module cannot be composed of targets which are outside of its base path.");
    }
  }

  @Value.Check
  protected void moduleDoesntDependOnItself() {
    ImmutableSet<BuildTarget> deps = getDependencies().keySet();
    for (TargetNode<?> targetNode : getTargets()) {
      Preconditions.checkArgument(!deps.contains(targetNode.getBuildTarget()));
    }
  }
}
