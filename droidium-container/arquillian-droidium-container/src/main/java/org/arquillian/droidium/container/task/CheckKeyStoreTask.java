/**
 * JBoss, Home of Professional Open Source
 * Copyright 2014, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the
 * distribution for a full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.arquillian.droidium.container.task;

import java.io.File;

import org.arquillian.droidium.container.configuration.AndroidSDK;
import org.arquillian.droidium.container.configuration.Validate;
import org.arquillian.droidium.platform.impl.DroidiumPlatformConfiguration;
import org.arquillian.spacelift.Spacelift;
import org.arquillian.spacelift.task.Task;

/**
 * Checks if some keystore from {@link DroidiumPlatformConfiguration} is valid, if not, it is created.
 *
 * @author <a href="smikloso@redhat.com">Stefan Miklosovic</a>
 *
 */
public class CheckKeyStoreTask extends Task<Object, Void> {

    private AndroidSDK androidSDK;

    public CheckKeyStoreTask sdk(AndroidSDK androidSDK) {
        this.androidSDK = androidSDK;
        return this;
    }

    @Override
    protected Void process(Object input) throws Exception {

        if (androidSDK == null) {
            throw new IllegalStateException("You have to set androidSdk via setter.");
        }

        if (!Validate.isReadable(new File(androidSDK.getPlatformConfiguration().getKeystore()))) {
            File defaultKeyStore = new File(getDefaultKeyStorePath());
            if (!Validate.isReadable(defaultKeyStore)) {
                Spacelift.task(CreateKeyStoreTask.class).keyStoreToCreate(defaultKeyStore).sdk(androidSDK).execute().await();
            } else {
                androidSDK.getPlatformConfiguration().setProperty("keystore", defaultKeyStore.getAbsolutePath());
            }
        }

        return null;
    }

    private String getDefaultKeyStorePath() {
        return androidSDK.getPlatformConfiguration().getAndroidHome() + "debug.keystore";
    }

}
