package org.acme;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

@ApplicationScoped
public class DesktopWindow {

    private static final Logger LOG = Logger.getLogger(DesktopWindow.class);
    private static final String OPENED_FLAG = "vozquark.window.opened";

    @ConfigProperty(name = "app.window", defaultValue = "false")
    boolean enabled;

    @ConfigProperty(name = "quarkus.http.port")
    int port;

    private volatile Process browser;

    void onStart(@Observes StartupEvent ev) {
        if (!enabled) {
            LOG.info("Janela desktop desativada (app.window=false)");
            return;
        }
        if (System.getProperty(OPENED_FLAG) != null) {
            return;   // evita abrir duas vezes (live reload)
        }
        System.setProperty(OPENED_FLAG, "true");

        String url = "http://localhost:" + port + "/";
        Thread t = new Thread(() -> launch(url), "app-window");
        t.setDaemon(true);
        t.start();
    }

    void onStop(@Observes ShutdownEvent ev) {
        Process p = browser;
        if (p != null && p.isAlive()) {
            p.descendants().forEach(ProcessHandle::destroy);
            p.destroy();
        }
    }

    private void launch(String url) {
        Path profile = Path.of(System.getProperty("user.home"), ".vozquark", "browser-profile");

        for (String exe : candidates()) {
            try {
                Process p = new ProcessBuilder(exe,
                        "--app=" + url,
                        "--user-data-dir=" + profile,
                        "--window-size=560,860",
                        "--no-first-run",
                        "--no-default-browser-check",
                        "--ozone-platform-hint=auto")
                        .redirectErrorStream(true)
                        .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                        .start();
                browser = p;
                LOG.infof("Janela aberta com: %s", exe);
                waitAndQuit(p);
                return;
            } catch (IOException e) {
                LOG.debugf("Não encontrado: %s", exe);
            }
        }
        LOG.warnf("Nenhum navegador compatível (Chromium, Chrome, Edge ou Brave) encontrado. "
                + "Acesse %s manualmente.", url);
    }

    /** Quando a janela é fechada, o processo do navegador termina e a aplicação encerra junto. */
    private void waitAndQuit(Process p) {
        try {
            long start = System.currentTimeMillis();
            p.waitFor();
            long secs = (System.currentTimeMillis() - start) / 1000;
            if (secs < 3) {
                LOG.warn("O navegador encerrou em menos de 3 s (possivelmente delegou a outra "
                        + "instância). O servidor continua rodando.");
                return;
            }
            LOG.info("Janela fechada. Encerrando a aplicação.");
            Quarkus.asyncExit();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static List<String> candidates() {
        String os = System.getProperty("os.name").toLowerCase();
        List<String> list = new ArrayList<>();
        if (os.contains("win")) {
            String pf = env("ProgramFiles", "C:\\Program Files");
            String pf86 = env("ProgramFiles(x86)", "C:\\Program Files (x86)");
            String local = env("LocalAppData", "");
            list.add(pf86 + "\\Microsoft\\Edge\\Application\\msedge.exe");
            list.add(pf + "\\Microsoft\\Edge\\Application\\msedge.exe");
            list.add(pf + "\\Google\\Chrome\\Application\\chrome.exe");
            list.add(pf86 + "\\Google\\Chrome\\Application\\chrome.exe");
            list.add(local + "\\Google\\Chrome\\Application\\chrome.exe");
        } else if (os.contains("mac")) {
            list.add("/Applications/Google Chrome.app/Contents/MacOS/Google Chrome");
            list.add("/Applications/Microsoft Edge.app/Contents/MacOS/Microsoft Edge");
            list.add("/Applications/Chromium.app/Contents/MacOS/Chromium");
        } else {
            list.addAll(List.of("chromium", "chromium-browser", "google-chrome-stable",
                    "google-chrome", "microsoft-edge-stable", "microsoft-edge",
                    "brave-browser", "brave"));
        }
        return list;
    }

    private static String env(String name, String fallback) {
        String v = System.getenv(name);
        return v == null ? fallback : v;
    }
}