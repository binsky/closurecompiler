package org.smartjava;
/*
 * Copyright 2013 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * @author <a href="http://tfox.org">Tim Fox</a>
 */

import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.platform.Verticle;

import java.io.*;
import java.net.URLDecoder;
import java.util.*;

public class PingVerticle extends Verticle {
    private static Map<String, String> db = new HashMap<>();
    private static final String UTF8 = "UTF-8";
    private static Set<String> files = new HashSet<>();

  public void start() {

      RouteMatcher rm = new RouteMatcher();

      rm.get("/", new Handler<HttpServerRequest>() {
          @Override
          public void handle(final HttpServerRequest httpServerRequest) {
              httpServerRequest.response().sendFile("/Users/broleg/git/playground/vertx-demo-1/src/main/templates/index.html");
          }
      });

      rm.get("/api/closure", new Handler<HttpServerRequest>() {
          @Override
          public void handle(HttpServerRequest httpServerRequest) {
              String key = httpServerRequest.params().get("key");
              if (key == null || !db.containsKey(key)) {
                  httpServerRequest.response().setStatusCode(404).end();
              }
              else {
                  httpServerRequest.response().setStatusCode(200).end(db.get(httpServerRequest.params().get("key")));
              }
          }
      });

      rm.post("/api/closure", new Handler<HttpServerRequest>() {
          @Override
          public void handle(final HttpServerRequest httpServerRequest) {
              httpServerRequest.dataHandler(new Handler<Buffer>() {
                  @Override
                  public void handle(Buffer buffer) {
                      Map<String, String> params = getParams(buffer);
                      try {
                          if (params.containsKey("key") && params.containsKey("input")) {
                              String key = URLDecoder.decode(params.get("key"), UTF8);
                              if (files.add(key)) {
                                  String outputJS =
                                          ClosureCompiler.compile(key, URLDecoder.decode(params.get("input"), UTF8));
                                  httpServerRequest.response().setStatusCode(200).end(outputJS);
                              } else {
                                  httpServerRequest.response().setStatusCode(409).
                                          setStatusMessage("Name conflict.").end();
                              }
                          }
                          else {
                              httpServerRequest.response().
                                      setStatusCode(409).end("File name and input code are required.");
                          }
                      }catch(IOException e){
                          e.printStackTrace();
                      }
                  }
              });
          }
      });

      rm.put("/api/closure", new Handler<HttpServerRequest>() {
          @Override
          public void handle(final HttpServerRequest httpServerRequest) {
              httpServerRequest.dataHandler(new Handler<Buffer>() {
                  @Override
                  public void handle(Buffer buffer) {
                      Map<String, String> params = getParams(buffer);
                      if (params.containsKey("key") && params.containsKey("input")) {
                          String key = null;
                          String js_code = null;
                          try {
                              key = URLDecoder.decode(params.get("key"), "UTF-8");
                              js_code = URLDecoder.decode(params.get("input"), "UTF-8");
                          } catch (UnsupportedEncodingException e) {
                              e.printStackTrace();
                          }
                          //closure compiler here
                          db.put(key, js_code);
                          httpServerRequest.response().setStatusCode(200).end(js_code);
                      }
                  }
              });
          }
      });

      //DOESN'T WORK PROPERLY
//      rm.delete("/api/closure", new Handler<HttpServerRequest>() {
//          @Override
//          public void handle(final HttpServerRequest httpServerRequest) {
//              httpServerRequest.dataHandler(new Handler<Buffer>() {
//                  @Override
//                  public void handle(Buffer buffer) {httpServerRequest.response().setStatusCode(200).end("bla");
//                      Map<String, String> params = getParams(buffer);
//                      if (params.containsKey("key")) {
//                          String key = params.get("key");
//                          db.remove(key);
//                          httpServerRequest.response().setStatusCode(200).end("bla");
//                      }
//                  }
//              });
//          }
//      });

      vertx.createHttpServer().requestHandler(rm).listen(8888);
      container.logger().info("Webserver started, listening on port: 8888");
  }

    private Map<String,String> getParams(Buffer buffer) {
        Map<String, String> params = new HashMap<>();
        String [] paramSplits = buffer.toString().split("&");
        String [] valueSplits;
        if (paramSplits.length>0) {
            for (String param : paramSplits) {
                valueSplits = param.split("=");
                if (valueSplits.length > 1) {
                    params.put(valueSplits[0], valueSplits[1]);
                }
            }
        }
        return params;
    }
}
