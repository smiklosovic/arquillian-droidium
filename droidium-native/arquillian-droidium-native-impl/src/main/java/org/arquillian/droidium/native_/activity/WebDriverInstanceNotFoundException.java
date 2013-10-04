/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat, Inc. and/or its affiliates, and individual
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
package org.arquillian.droidium.native_.activity;

/**
 * Thrown in case there is not any WebDriver instance found for some Android activity through which it will be controlled.
 *
 * @author <a href="mailto:smikloso@redhat.com">Stefan Miklosovic</a>
 *
 */
public class WebDriverInstanceNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 4260016538030217837L;

    public WebDriverInstanceNotFoundException() {
    }

    /**
     * @param message
     */
    public WebDriverInstanceNotFoundException(String message) {
        super(message);
    }

    /**
     * @param cause
     */
    public WebDriverInstanceNotFoundException(Throwable cause) {
        super(cause);
    }

    /**
     * @param message
     * @param cause
     */
    public WebDriverInstanceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

}
