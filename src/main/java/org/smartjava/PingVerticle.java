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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

/*
This is a simple Java verticle which receives `ping` messages on the event bus and sends back `pong` replies
 */
public class PingVerticle extends Verticle {
    private static HashMap<String, String> db = new HashMap<String, String>();
    static {
        //FOR TESTS ONLY
        db.put("example.js", "$(document).ready(function(){\n" +
                "            $(\"#get\").click(function(e){\n" +
                "                var js_key = $(\"js_key\").val();\n" +
                "                $.ajax({\n" +
                "                    url : \"api/closure\",\n" +
                "                    port : \"8888\",\n" +
                "                    type : \"GET\",\n" +
                "                    dataType: \"text\",\n" +
                "                    data : {\n" +
                "                        key : js_key,\n" +
                "                    },\n" +
                "                    success : function(data) {\n" +
                "                        $(\"#output\").val(data);\n" +
                "                    },\n" +
                "                    error : function(xhr,errmsg,err) {\n" +
                "                        alert(xhr.status + \": \" + xhr.responseText);\n" +
                "                    }\n" +
                "                });\n" +
                "                e.preventDefault();\n" +
                "            });");
    }

  public void start() {

      RouteMatcher rm = new RouteMatcher();

      rm.get("/", new Handler<HttpServerRequest>() {
          @Override
          public void handle(final HttpServerRequest httpServerRequest) {
              httpServerRequest.response().sendFile("/Users/broleg/git/playground/vertx-demo-1/src/main/templates/index.html");
//              /Users/broleg/git/playground/vertx-demo-1/src/main/java/org/smartjava/PingVerticle.java
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
//                      com.google.javascript.jscomp.Compiler compiler = new com.google.javascript.jscomp.Compiler();
//                      CompilerOptions options = new CompilerOptions();
//                      CompilationLevel.SIMPLE_OPTIMIZATIONS.setOptionsForCompilationLevel(options);
                      if (params.containsKey("key")) {
                          String key = params.get("key");
                          if (!db.containsKey(key) && params.containsKey("input")) {
                              String js_code = null;
                              try {
                                  js_code = URLDecoder.decode(params.get("input"), "UTF-8");
                              } catch (UnsupportedEncodingException e) {
                                  e.printStackTrace();
                              }
                              //closure compiler here
                              db.put(key, js_code);
                              httpServerRequest.response().setStatusCode(200).end(js_code);
                          }
                          else {
                              httpServerRequest.response().setStatusCode(409).setStatusMessage("There is a name conflict").end();
                          }
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
        Map<String, String> params = new HashMap<String, String>();
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
