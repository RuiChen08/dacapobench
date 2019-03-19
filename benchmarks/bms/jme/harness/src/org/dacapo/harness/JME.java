/*
 * Copyright (c) 2018 The Australian National University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0.
 * You may obtain the license at
 *
 *    http://www.opensource.org/licenses/apache2.0.php
 */

package org.dacapo.harness;

import org.dacapo.parser.Config;
import java.io.File;

public class JME extends Benchmark{

    private String[] args;

    public JME(Config config, File scratch, File data) throws Exception {
        super(config, scratch);
        Class<?> clazz = Class.forName("jme3test.TestChooser", true, loader);
        this.method = clazz.getMethod("main", String[].class);
    }

    @Override
    protected void prepare(String size) throws Exception {
        super.prepare(size);
        args = config.preprocessArgs(size, scratch, data);
    }

    @Override
    public void iterate(String size) throws Exception {
        this.method.invoke(null, (Object) args);
    }
}
