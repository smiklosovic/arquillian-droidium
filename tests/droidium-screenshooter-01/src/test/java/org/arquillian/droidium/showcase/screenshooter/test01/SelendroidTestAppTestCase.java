/**
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
package org.arquillian.droidium.showcase.screenshooter.test01;

import java.io.File;

import org.arquillian.droidium.container.api.AndroidDevice;
import org.arquillian.droidium.native_.api.Instrumentable;
import org.arquillian.extension.recorder.screenshooter.Screenshooter;
import org.arquillian.extension.recorder.screenshooter.ScreenshotType;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 *
 * @author <a href="mailto:smikloso@redhat.com">Stefan Miklosovic</a>
 *
 */
@RunWith(Arquillian.class)
@RunAsClient
public class SelendroidTestAppTestCase {

    @ArquillianResource
    Screenshooter screenshooter;

    @Drone
    WebDriver driver;

    @Deployment
    @Instrumentable
    public static Archive<?> createDeployment() {
        return ShrinkWrap.createFromZipFile(JavaArchive.class, new File(System.getProperty("selendroid.test.app")));
    }

    private static final String USER_NAME = "john";

    private static final String USER_EMAIL = "john@doe.com";

    private static final String USER_PASSWORD = "p4ssw0rd";

    private static final String USER_REAL_NAME = "John Doe";

    private static final String USER_REAL_OLD = "Mr. Burns";

    private static final String USER_PRGRAMMING_LANGUAGE = "Scala";

    /**
     * Simple test which tries to register some user.
     *
     * @param screenshooter takes screenshots of {@code device}
     * @param device Android device itself, it is not needed in tests as such since we interact only with {@code WebDriver}
     *        injection.
     * @param driver {@code WebDriver} injection which sends commands to Selendroid server installed on the Android device.
     */
    @Test
    @InSequence(1)
    public void test01(@ArquillianResource AndroidDevice device) {

        device.getActivityManager().startActivity("io.selendroid.testapp.HomeScreenActivity");

        // where to save taken screenshots, by default to target/screenshots
        screenshooter.setScreenshotTargetDir("target/screenshots-1");

        // take it!
        screenshooter.takeScreenshot();

        // Go to user registration
        driver.findElement(By.id("startUserRegistration")).click();

        // take it as GIF, by default, it is taken as PNG
        screenshooter.setMessage("taking gif").takeScreenshot(ScreenshotType.GIF);

        // enter nick
        WebElement userName = driver.findElement(By.id("inputUserName"));
        userName.sendKeys(USER_NAME);
        Assert.assertEquals(userName.getText(), USER_NAME);

        // enter e-mail
        WebElement inputEmail = driver.findElement(By.id("inputEmail"));
        inputEmail.sendKeys(USER_EMAIL);
        Assert.assertEquals(inputEmail.getText(), USER_EMAIL);

        // enter password
        WebElement inputPassword = driver.findElement(By.id("inputPassword"));
        inputPassword.sendKeys(USER_PASSWORD);
        Assert.assertEquals(inputPassword.getText(), USER_PASSWORD);

        // check value in name field, clear it and write new one
        WebElement inputName = driver.findElement(By.id("inputName"));
        Assert.assertEquals(inputName.getText(), USER_REAL_OLD);
        inputName.clear();
        inputName.sendKeys(USER_REAL_NAME);
        Assert.assertEquals(inputName.getText(), USER_REAL_NAME);

        // enter favorite language
        driver.findElement(By.id("input_preferedProgrammingLanguage")).click();
        driver.findElement(By.linkText(USER_PRGRAMMING_LANGUAGE)).click();

        // accept adds checkbox
        WebElement acceptAddsCheckbox = driver.findElement(By.id("input_adds"));
        Assert.assertEquals(acceptAddsCheckbox.isSelected(), false);

        acceptAddsCheckbox.click();

        screenshooter.setScreenshotTargetDir("target/screenshots-2");

        // from now on, take all images as BMP if not specified otherwise
        screenshooter.setScreenshotType(ScreenshotType.BMP);

        // you can name it, it will be PNG image by default
        screenshooter.takeScreenshot("myscreenshot1");

        // register
        driver.findElement(By.id("btnRegisterUser")).click();

        // or you can specify file format as well
        screenshooter.takeScreenshot("myscreenshot2", ScreenshotType.JPEG);

    }
}
