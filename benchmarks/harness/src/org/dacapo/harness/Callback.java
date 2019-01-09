/*
 * Copyright (c) 2006, 2009 The Australian National University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0.
 * You may obtain the license at
 * 
 *    http://www.opensource.org/licenses/apache2.0.php
 */
package org.dacapo.harness;

import org.dacapo.harness.CommandLineArgs.Methodology;
import org.dacapo.parser.Config;
import java.util.Arrays;
import java.util.ArrayList;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.File;


/**
 * date:  $Date: 2009-12-24 11:19:36 +1100 (Thu, 24 Dec 2009) $
 * id: $Id: Callback.java 738 2009-12-24 00:19:36Z steveb-oss $
 */
public class Callback {

  /**
   * Support for timing methodologies that have timing and warmup runs.
   */
  protected enum Mode {
    WARMUP, TIMING
  };

  protected Mode mode;

  /**
   * The parsed command line arguments
   */
  protected final CommandLineArgs args;

  /**
   * Iterations of the current benchmark completed so far
   */
  protected int iterations;

  /**
   * Times for the last n iterations of the current benchmark
   */
  protected long[] times;

  /**
   * 
   */
  protected long elapsed;

  boolean verbose = false;

  /**
   *print out all the times queries spend
   */
  boolean queryVerbose = false;

  /**
   *the file in which the query output will be
   */
  protected File queryFile;

  /**
   * Create a new callback.
   * 
   * @param args The parsed command-line arguments.
   */
  public Callback(CommandLineArgs args) {
    this.args = args;
    if (args.getMethodology() == Methodology.CONVERGE) {
      times = new long[args.getWindow()];
    }
    verbose |= args.getDebug();
  }

  public void init(Config config) {
    if (verbose)
      System.out.println("Initializing callback");
    iterations = 0;

    switch (args.getMethodology()) {
    case ITERATE:
      if (args.getIterations() == 1)
        mode = Mode.TIMING;
      else
        mode = Mode.WARMUP;
      break;
    case CONVERGE:
      if (args.getWindow() == 0)
        mode = Mode.TIMING;
      else
        mode = Mode.WARMUP;
    }

    if (times != null)
      for (int i = 0; i < times.length; i++)
        times[i] = 0;

    this.queryFile = null;
  }

  /**
   * This method governs the benchmark iteration process. The test harness will
   * run the benchmark repeatedly until this method returns 'false'.
   * 
   * The default methodologies consist of 0 or more 'warmup' iterations,
   * followed by a single timing iteration.
   * 
   * @return Whether to run another iteration.
   */
  public boolean runAgain() {
    if (verbose)
      System.out.println("runAgain");
    /* Always quit immediately after the timing iteration */
    if (!isWarmup())
      return false;

    iterations++;
    if (verbose)
      System.out.println("iterations = " + iterations);
    switch (args.getMethodology()) {
    case ITERATE:
      if (iterations == args.getIterations() - 1)
        mode = Mode.TIMING;
      if (verbose)
        System.out.println("mode = " + mode);
      return true;

    case CONVERGE:
      /* If we've exceeded the maximum iterations, exit */
      if (iterations >= args.getMaxIterations()) {
        System.err.println("Benchmark failed to converge.");
        return false;
      }


      /* Maintain the sliding window of execution times */
      times[(iterations - 1) % args.getWindow()] = elapsed;


      /* If we haven't filled the window, repeat immediately */
      if (iterations < args.getWindow())
        return true;

      /* Optionally report on progress towards convergence */
      if (iterations >= args.getWindow() && args.getVerbose()) {
        System.err.printf("Variation %4.2f%% achieved after %d iterations, target = %4.2f%%\n", TestHarness.coeff_of_var(times) * 100, iterations, args
            .getTargetVar() * 100);
      }

      /* Not yet converged, repeat in warmup mode */
      if (TestHarness.coeff_of_var(times) > args.getTargetVar())
        return true;

      /* If we've fallen through to here, we must have converged */
      mode = Mode.TIMING;
      return true;
    }

    // We should never fall through
    assert false;
    return false; // Keep javac happy
  }

  public boolean isWarmup() {
    return mode == Mode.WARMUP;
  }

  public void setQueryFile(File file) throws IOException{
          this.queryFile = file;
          if(!file.exists()){
            if(!file.createNewFile())
            {

            }
          }
  }

  /**
   * Start the timer and announce the begining of an iteration
   */
  public void start(String benchmark) {
    start(benchmark, mode == Mode.WARMUP);
  };

  protected void start(String benchmark, boolean warmup) {
    System.err.print("===== DaCapo " + TestHarness.getBuildVersion() + " " + benchmark + " starting ");
    System.err.println((warmup ? ("warmup " + (iterations + 1) + " ") : "") + "=====");
    System.err.flush();
  }

  private static long[][] txstart;
  private static long[][] txduration;
  private static long[][] oldTxStart;
  private static long[][] oldTxDuration;
  private static boolean[] isExecuted;
  public static void setThreadCount(int totalThreads){
    txstart = new long[totalThreads][];
    txduration = new long[totalThreads][];
    oldTxStart = new long[totalThreads][];
    oldTxDuration = new long[totalThreads][];
    isExecuted = new boolean[totalThreads];
  }

  public static void setTxCount(int id, int txNum)throws ArrayIndexOutOfBoundsException{
    if(isExecuted[id]){
      oldTxDuration[id] = txduration[id].clone();
      oldTxStart[id] = txstart[id].clone();
      txstart[id] = new long[oldTxStart[id].length+txNum];
      txduration[id] = new long[oldTxDuration[id].length+txNum];
      for(int i = 0; i < oldTxDuration[id].length; i++){
        txduration[id][i] = oldTxDuration[id][i];
        txstart[id][i] = oldTxStart[id][i];
      }
    }else {
      txstart[id] = new long[txNum];
      txduration[id] = new long[txNum];
      isExecuted[id] = true;
    }

  }

  public static void starttx(int id, int count) throws ArrayIndexOutOfBoundsException {
      txstart[id][count] = System.nanoTime();
  }

  public static void stoptx(int id, int count) throws ArrayIndexOutOfBoundsException {
    txduration[id][count] = System.nanoTime() - txstart[id][count];
  }

  public void stop(long duration) {
    stop(duration, mode == Mode.WARMUP);
  }

  public void stop(long duration, boolean warmup) {
    elapsed = duration;
  }

  /* Announce completion of the benchmark (pass or fail) */
  public void complete(String benchmark, boolean valid) {
    complete(benchmark, valid, mode == Mode.WARMUP);
  };

  protected void complete(String benchmark, boolean valid, boolean warmup) {
    if(txduration!=null) {
      ArrayList<Long> times = new ArrayList<>();
      boolean isThe1st = true;
      String queryTimes = "query-times = [ ";
      for(long [] txtimes: txduration){
        if(txtimes!=null)
          for(long txtime: txtimes) {

            if(txtime!=0) {
              if(isThe1st)
                isThe1st = false;
              else
                queryTimes = queryTimes + ", ";
              queryTimes = queryTimes + txtime;
              times.add(txtime);
            }
          }
      }
      queryTimes = queryTimes + " ]";

      long [] tmArray = times.stream().mapToLong(t->t.longValue()).toArray();
      Arrays.sort(tmArray);

      String pencentilesOutput = "===== Query percentiles 99: " + tmArray[(int) ((tmArray.length) * 0.99)]+" mrcsc, "+
                "95: " + tmArray[(int) ((tmArray.length) * 0.95)]+" mcrsc, "+
                "90: " + tmArray[(int) ((tmArray.length) * 0.90)]+" mcrsc, "+
                "50: " + tmArray[(int) ((tmArray.length) * 0.50)]+" mcrsc =====\n";



      if(queryFile!=null){
        try {
          FileOutputStream outputStream = new FileOutputStream(queryFile);

          outputStream.write(pencentilesOutput.getBytes());

          if (queryVerbose) {
            outputStream.write(queryTimes.getBytes());
          }
          outputStream.flush();
          outputStream.close();
        }catch (IOException e){
          System.out.println(e);
        }
      }else{
        System.out.println(pencentilesOutput);
        if(queryVerbose){
          System.out.println(queryTimes);
        }
      }
    }
    System.err.print("===== DaCapo " + TestHarness.getBuildVersion() + " " + benchmark);
    if (valid) {
      System.err.print(warmup ? (" completed warmup " + (iterations + 1) + " ") : " PASSED ");
      System.err.print("in " + elapsed + " mesc ");
    } else {
      System.err.print(" FAILED " + (warmup ? "warmup " : ""));
    }
    System.err.println("=====");
    System.err.flush();
  }

}
