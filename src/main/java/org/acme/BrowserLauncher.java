// package org.acme;

// import java.util.List;

// import org.eclipse.microprofile.config.inject.ConfigProperty;
// import org.jboss.logging.Logger;

// import io.quarkus.runtime.StartupEvent;
// import jakarta.enterprise.context.ApplicationScoped;
// import jakarta.enterprise.event.Observes;

// @ApplicationScoped
// public class BrowserLauncher {

//     private static final Logger LOG = Logger.getLogger(BrowserLauncher.class);
//     private static final String OPENED_FLAG = "vozquark.browser.opened";

//     @ConfigProperty(name = "app.open-browser", defaultValue = "true")
//     boolean openBrowser;

//     @ConfigProperty(name = "quarkus.http.port")
//     int port;

//     void onStart(@Observes StartupEvent ev) {
//         if (!openBrowser) {
//             return;
//         }
//         // No modo dev, o live reload reinicia a app; este flag evita abrir várias abas.
//         if (System.getProperty(OPENED_FLAG) != null) {
//             return;
//         }
//         System.setProperty(OPENED_FLAG, "true");

//         String url = "http://localhost:" + port;
//         String os = System.getProperty("os.name").toLowerCase();

//         List<String> cmd;
//         if (os.contains("win")) {
//             cmd = List.of("rundll32", "url.dll,FileProtocolHandler", url);
//         } else if (os.contains("mac")) {
//             cmd = List.of("open", url);
//         } else {
//             cmd = List.of("xdg-open", url);
//         }

//         try {
//             new ProcessBuilder(cmd).start();
//             LOG.infof("Abrindo o navegador em %s", url);
//         } catch (Exception e) {
//             LOG.warnf("Não foi possível abrir o navegador automaticamente. Acesse %s", url);
//         }
//     }
// }