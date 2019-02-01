/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api.internal.artifacts.transform;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.artifacts.transform.ArtifactTransform;
import org.gradle.api.internal.attributes.ImmutableAttributes;
import org.gradle.api.internal.tasks.properties.InputFilePropertyType;
import org.gradle.internal.file.PathToFileResolver;
import org.gradle.internal.fingerprint.AbsolutePathInputNormalizer;
import org.gradle.internal.fingerprint.CurrentFileCollectionFingerprint;
import org.gradle.internal.fingerprint.FileCollectionFingerprinterRegistry;
import org.gradle.internal.hash.HashCode;
import org.gradle.internal.instantiation.InstantiatorFactory;
import org.gradle.internal.isolation.Isolatable;
import org.gradle.internal.reflect.Instantiator;

import java.io.File;
import java.util.List;

public class LegacyTransformer extends AbstractTransformer<ArtifactTransform> {
    private static final String PRIMARY_INPUT_PROPERTY_NAME = "primaryInput";
    private static final String DEPENDENCIES_PROPERTY_NAME = "dependencies";

    private final Isolatable<Object[]> parameters;
    private final Instantiator instantiator;

    public LegacyTransformer(Class<? extends ArtifactTransform> implementationClass, Isolatable<Object[]> parameters, HashCode inputsHash, InstantiatorFactory instantiatorFactory, ImmutableAttributes fromAttributes) {
        super(implementationClass, inputsHash, fromAttributes);
        this.instantiator = instantiatorFactory.inject();
        this.parameters = parameters;
    }

    public boolean requiresDependencies() {
        return false;
    }

    @Override
    public List<File> transform(File primaryInput, File outputDir, ArtifactTransformDependencies dependencies) {
        ArtifactTransform transformer = newTransformer();
        transformer.setOutputDirectory(outputDir);
        List<File> outputs = transformer.transform(primaryInput);
        if (outputs == null) {
            throw new InvalidUserDataException("Transform returned null result.");
        }
        return validateOutputs(primaryInput, outputDir, ImmutableList.copyOf(outputs));
    }

    @Override
    public ImmutableSortedMap<String, CurrentFileCollectionFingerprint> getInputFileFingerprints(File primaryInput, ArtifactTransformDependencies dependencies, FileCollectionFingerprinterRegistry fileCollectionFingerprinterRegistry, PathToFileResolver resolver) {
        ImmutableSortedMap.Builder<String, CurrentFileCollectionFingerprint> builder = ImmutableSortedMap.naturalOrder();
        builder.put(PRIMARY_INPUT_PROPERTY_NAME, fingerprintInput(primaryInput, AbsolutePathInputNormalizer.class, InputFilePropertyType.FILES, fileCollectionFingerprinterRegistry, resolver));
        return builder.build();
    }

    private ArtifactTransform newTransformer() {
        return instantiator.newInstance(getImplementationClass(), parameters.isolate());
    }
}
