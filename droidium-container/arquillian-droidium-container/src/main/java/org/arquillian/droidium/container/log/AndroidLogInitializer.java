/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.arquillian.droidium.container.log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.arquillian.droidium.container.api.AndroidDevice;
import org.arquillian.droidium.container.configuration.AndroidContainerConfiguration;
import org.arquillian.droidium.container.configuration.AndroidSDK;
import org.arquillian.droidium.container.spi.event.AndroidBridgeTerminated;
import org.arquillian.droidium.container.spi.event.AndroidDeviceReady;
import org.arquillian.spacelift.Spacelift;
import org.arquillian.spacelift.execution.Execution;
import org.arquillian.spacelift.execution.ExecutionException;
import org.arquillian.spacelift.process.Command;
import org.arquillian.spacelift.process.CommandBuilder;
import org.arquillian.spacelift.task.Task;
import org.arquillian.spacelift.task.os.CommandTool;
import org.jboss.arquillian.container.spi.context.annotation.ContainerScoped;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;

/**
 *
 *
 * @author <a href="mailto:tkriz@redhat.com">Tadeas Kriz</a>
 */
// FIXME this class is leaking resources - 3 processes are executed
public class AndroidLogInitializer {
    private static final Logger logger = Logger.getLogger(AndroidLogInitializer.class.getName());

    @Inject
    @ContainerScoped
    private InstanceProducer<LogcatHelper> logcatHelper;

    @Inject
    private Instance<AndroidContainerConfiguration> configuration;

    @Inject
    private Instance<AndroidSDK> androidSDK;

    @Inject
    private Instance<AndroidDevice> androidDevice;

    private Execution<Writer> logcatExecution;

    public void initAndroidLog(@Observes AndroidDeviceReady event) {
        logger.info("Initializing Android LogcatReader");

        if (logcatHelper.get() == null) {
            logcatHelper.set(new LogcatHelper(configuration.get(), androidDevice.get()));
        }

        this.logcatExecution = Spacelift.task(LogcatReader.class)
            .androidSdk(androidSDK.get())
            .containerConfiguration(configuration.get())
            .androidDevice(androidDevice.get())
            .writer(logcatHelper.get().prepareWriter())
            .execute();
    }

    public void terminateAndroidLog(@Observes AndroidBridgeTerminated event) {
        logcatExecution.terminate();

        Writer w = logcatExecution.await();
        if(w != null) {
            try {
                w.close();
            }
            catch (IOException e) {
            }
        }
    }

    private static class LogcatReader extends Task<Object,Writer> {
        private AndroidContainerConfiguration configuration;
        private AndroidSDK androidSDK;
        private AndroidDevice androidDevice;

        private Writer writer;

        private Pattern pattern;

        private List<String> whiteList = new ArrayList<String>();
        private List<String> blackList = new ArrayList<String>();
        private Map<Integer, String> processMap = new HashMap<Integer, String>();

        public LogcatReader androidDevice(AndroidDevice device) {
            this.androidDevice = device;
            return this;
        }


        public LogcatReader androidSdk(AndroidSDK sdk) {
            this.androidSDK = sdk;
            return this;
        }

        public LogcatReader containerConfiguration(AndroidContainerConfiguration configuration) {
            this.configuration = configuration;

            if (configuration.getLogPackageWhitelist() != null) {
                String[] whiteList = configuration.getLogPackageWhitelist().split(",");
                for (String packageName : whiteList) {
                    this.whiteList.add(escapePackageName(packageName));
                }
            }

            if (configuration.getLogPackageBlacklist() != null) {
                String[] blackList = configuration.getLogPackageBlacklist().split(",");
                for (String packageName : blackList) {
                    this.blackList.add(escapePackageName(packageName));
                }
            }
            return this;
        }

        public LogcatReader writer(Writer writer) {
            this.writer = writer;
            return this;
        }

        @Override
        protected Writer process(Object input) throws Exception {
            if (writer == null) {
                return null;
            }

            try {
                Command command = new CommandBuilder(androidSDK.getAdbPath())
                    .parameter("-s")
                    .parameter(androidDevice.getSerialNumber())
                    .parameter("logcat")
                    .parameter("-c")
                    .build();

                ProcessBuilder builder = new ProcessBuilder(command.getFullCommand());
                Process process = builder.start();

                command = new CommandBuilder(androidSDK.getAdbPath())
                    .parameter("-s")
                    .parameter(androidDevice.getSerialNumber())
                    .parameter("logcat")
                    .parameter("*:" + configuration.getLogLevel())
                    .build();

                builder = new ProcessBuilder(command.getFullCommand());
                process = builder.start();

                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line = null;
                while ((line = reader.readLine()) != null) {
                    if (shouldWrite(line)) {
                        writer.write(line);
                        writer.flush();
                    }
                }
                writer.close();
                reader.close();
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Error with logcat logging!", e);
            }

            return writer;
        }

        private String escapePackageName(String packageName) {
            return packageName
                .replace("\\", "\\\\")
                .replace(".", "\\.")
                .replace("[", "\\[")
                .replace("]", "\\]")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replace("?", "\\?")
                .replace("+", "\\+")
                .replace("*", ".*?");
        }

        private boolean shouldWrite(String line) {
            if (!configuration.isLogFilteringEnabled()) {
                return true;
            }

            if (pattern == null) {
                // This pattern will fetch us process id from logcat line
                pattern = Pattern.compile("./.+?\\(([\\s0-9]+?)\\):.*");
            }

            Matcher matcher = pattern.matcher(line);
            if (!matcher.matches()) {
                return false;
            }

            String processIdString = matcher.group(1).trim();
            Integer processId = Integer.valueOf(processIdString);

            if (!processMap.containsKey(processId)) {
                loadProcessMap();
            }

            String processName = processMap.get(processId);
            if (processName == null) {
                processName = "";
            }

            for (String regex : whiteList) {
                if (processName.matches(regex)) {
                    return true;
                }
            }

            for (String regex : blackList) {
                if (processName.matches(regex)) {
                    return false;
                }
            }

            return true;
        }

        private void loadProcessMap() {
            try {
                processMap.clear();

                Command command = new CommandBuilder(androidSDK.getAdbPath())
                    .parameter("-s")
                    .parameter(androidDevice.getSerialNumber())
                    .parameter("shell")
                    .parameter("ps")
                    .build();

                List<String> runningProcesses = Spacelift.task(CommandTool.class)
                    .addEnvironment(androidSDK.getPlatformConfiguration().getAndroidSystemEnvironmentProperties())
                    .command(command).execute().await().output();

                Pattern pattern = Pattern
                    .compile(".*?\\s+([0-9]+)\\s+[0-9]+\\s+[0-9]+\\s+[0-9]+\\s+[0-9a-f]+\\s+[0-9a-f]+\\s.?\\s(.*)");
                for(String line: runningProcesses) {
                    Matcher matcher = pattern.matcher(line);

                    if (!matcher.matches()) {
                        continue;
                    }

                    Integer processId = Integer.valueOf(matcher.group(1));
                    String processName = matcher.group(2);

                    processMap.put(processId, processName);
                }
            } catch (ExecutionException e) {
                logger.log(Level.SEVERE, "Couldn't load process map!", e);

            }
        }
    }

}