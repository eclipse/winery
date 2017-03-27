/*******************************************************************************
 * Copyright (c) 2017 University of Stuttgart.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and the Apache License 2.0 which both accompany this distribution,
 * and are available at http://www.eclipse.org/legal/epl-v10.html
 * and http://www.apache.org/licenses/LICENSE-2.0
 *
 * Contributors:
 *     Oliver Kopp - initial API and implementation
 *******************************************************************************/
package org.eclipse.winery.repository.resources;

import java.io.InputStream;
import java.util.Scanner;

import org.eclipse.winery.common.Util;
import org.eclipse.winery.repository.PrefsTestEnabledGitBackedRepository;
import org.eclipse.winery.repository.WineryUsingHttpServer;

import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.eclipse.jetty.server.Server;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.skyscreamer.jsonassert.JSONAssert;
import org.xmlunit.matchers.CompareMatcher;

import static io.restassured.RestAssured.given;
import static org.hamcrest.core.Is.is;

public abstract class AbstractResourceTest {

	// with trailing /
	private static final String PREFIX = "/winery/";

	private static Git git;

	private static Server server;

	@BeforeClass
	public static void init() throws Exception {
		// enable git-backed repository
		PrefsTestEnabledGitBackedRepository prefsTestEnabledGitBackedRepository = new PrefsTestEnabledGitBackedRepository();
		git = prefsTestEnabledGitBackedRepository.git;
		server = WineryUsingHttpServer.createHttpServer();
		server.start();
	}

	@AfterClass
	public static void shutdown() throws Exception {
		server.stop();
	}

	protected void setRevisionTo(String ref) throws GitAPIException {
		// TODO: newer JGit version: setForce(true)
		git.clean().setCleanDirectories(true).call();

		this.git.reset()
				.setMode(ResetCommand.ResetType.HARD)
				.setRef(ref)
				.call();
	}

	protected String readFromClasspath(String fileName) {
		final InputStream inputStream = ClassLoader.getSystemClassLoader().getResourceAsStream(fileName);
		if (inputStream == null) {
			throw new IllegalStateException("Could not find " + fileName + " on classpath");
		}
		return new Scanner(inputStream, "UTF-8").useDelimiter("\\A").next();
	}

	protected RequestSpecification start() {
		return given()
				.log()
				.ifValidationFails();
	}

	private String callURL(String restURL) {
		return PREFIX + Util.URLdecode(restURL);
	}

	private boolean isXml(String fileName) {
		return (fileName.endsWith("xml"));
	}

	private ContentType getAccept(String fileName) {
		if (isXml(fileName)) {
			return ContentType.XML;
		} else {
			return ContentType.JSON;
		}
	}

	public void assertNotFound(String restURL) {
		try {
			start()
					.get(callURL(restURL))
					.then()
					.statusCode(404);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void assertGet(String restURL, String fileName) {
		try {
			String expectedStr = readFromClasspath(fileName);
			final String receivedStr = start()
					.accept(getAccept(fileName))
					.get(callURL(restURL))
					.then()
					.log()
					.ifError()
					.statusCode(200)
					.extract()
					.response()
					.getBody()
					.asString();
			if (isXml(fileName)) {
				org.hamcrest.MatcherAssert.assertThat(receivedStr, CompareMatcher.isIdenticalTo(expectedStr).ignoreWhitespace());
			} else {
				JSONAssert.assertEquals(
						expectedStr,
						receivedStr,
						true);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void assertGetSize(String restURL, int size) {
		start()
				.get(callURL(restURL))
				.then()
				.log()
				.ifError()
				.statusCode(200)
				.body("size()", is(size));
	}

	public void assertPut(String restURL, String fileName) {
		String contents = readFromClasspath(fileName);
		start()
				.body(contents)
				.contentType(getAccept(fileName))
				.put(callURL(restURL))
				.then()
				.statusCode(204);
	}

	public void assertPost(String restURL, String fileName) {
		String contents = readFromClasspath(fileName);
		start()
				.body(contents)
				.contentType(getAccept(fileName))
				.post(callURL(restURL))
				.then()
				.statusCode(201);
	}

	public void assertPost(String restURL, String namespace, String name) {
		start()
				.formParam("namespace", namespace)
				.formParam("name", name)
				.post(callURL(restURL))
				.then()
				.statusCode(201);
	}

	protected void assertDelete(String restURL) {
		try {
			start()
					.delete(callURL(restURL))
					.then()
					.statusCode(204);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
