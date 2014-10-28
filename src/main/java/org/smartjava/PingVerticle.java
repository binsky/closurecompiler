package org.smartjava;

import com.google.javascript.jscomp.SourceFile;
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
    private static final String ERR_NAME_CONFLICT = "Name conflict";
    private static final String ERR_INP_REQUIERED = "File name and input code are required.";

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
              if (files.contains(key)) {
                  try {
                      httpServerRequest.response().setStatusCode(200).end(FileManager.getJSCode(key, UTF8));
                  } catch (IOException e) {
                      httpServerRequest.response().end();
                  }
              }
              else {
                  httpServerRequest.response().setStatusCode(404).end();
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
                              String fileName = URLDecoder.decode(params.get("key"), UTF8);
                              if (files.add(fileName)) {

                                  List<SourceFile> sourceFiles =
                                          FileManager.createSourceFile(fileName, URLDecoder.decode(params.get("input"), UTF8));
                                  String outputJS = ClosureCompiler.compile(sourceFiles);
                                  FileManager.createClosuredFile(fileName, outputJS);
                                  httpServerRequest.response().setStatusCode(200).end(outputJS);
                              } else {
                                  httpServerRequest.response().setStatusCode(409).
                                          setStatusMessage(ERR_NAME_CONFLICT).end();
                              }
                          }
                          else {
                              httpServerRequest.response().
                                      setStatusCode(409).setStatusMessage(ERR_INP_REQUIERED).end();
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
                      try {
                          if (params.containsKey("key") && params.containsKey("input")) {
                              String fileName = URLDecoder.decode(params.get("key"), UTF8);
                              files.add(fileName);
                              List<SourceFile> sourceFiles =
                                      FileManager.createSourceFile(fileName, URLDecoder.decode(params.get("input"), UTF8));
                              String outputJS =
                                      ClosureCompiler.compile(sourceFiles);
                              FileManager.createClosuredFile(fileName, outputJS);
                              httpServerRequest.response().setStatusCode(200).end(outputJS);
                          }
                          else {
                              httpServerRequest.response().
                                      setStatusCode(409).setStatusMessage(ERR_INP_REQUIERED).end();
                          }
                      }catch(IOException e){
                          e.printStackTrace();
                      }
                  }
              });
          }
      });

      rm.delete("/api/closure/:filename", new Handler<HttpServerRequest>() {
          @Override
          public void handle(final HttpServerRequest httpServerRequest) {
              String fileName = httpServerRequest.params().get("filename");
              if (files.remove(fileName)) {
                  FileManager.deleteFiles(fileName);
                  httpServerRequest.response().setStatusCode(200).end();
              }
              else {
                  httpServerRequest.response().setStatusCode(404).end();
              }
          }
      });

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
