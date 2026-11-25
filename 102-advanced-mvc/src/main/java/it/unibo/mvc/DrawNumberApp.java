package it.unibo.mvc;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Application entry point for DrawNumber game.
 */
public final class DrawNumberApp implements DrawNumberViewObserver {
    private static final String FILE_NAME = "config.yml";

    private final DrawNumber model;
    private final List<DrawNumberView> views;

    /**
     * Constructor.
     *
     * @param views
     *              the views to attach
     */
    public DrawNumberApp(final DrawNumberView... views) {
        /*
         * Side-effect proof
         */
        this.views = Arrays.asList(Arrays.copyOf(views, views.length));
        for (final DrawNumberView view : views) {
            view.setObserver(this);
            view.start();
        }
        Configuration configuration = buildFromFile();
        if (!configuration.isConsistent()) {
            displayError("Given configuration is not consistent, using default instead");
            final Configuration.Builder def = new Configuration.Builder();
            configuration = def.build();
        }
        this.model = new DrawNumberImpl(configuration);
    }

    @Override
    public void newAttempt(final int n) {
        try {
            final DrawResult result = model.attempt(n);
            for (final DrawNumberView view : views) {
                view.result(result);
            }
        } catch (final IllegalArgumentException e) {
            for (final DrawNumberView view : views) {
                view.numberIncorrect();
            }
        }
    }

    @Override
    public void resetGame() {
        this.model.reset();
    }

    @Override
    @SuppressFBWarnings(value = "DM_EXIT", justification = "Acceptable for exercising purposes.")
    public void quit() {
        /*
         * A bit harsh. A good application should configure the graphics to exit by
         * natural termination when closing is hit. To do things more cleanly, attention
         * should be paid to alive threads, as the application would continue to persist
         * until the last thread terminates.
         */
        System.exit(0);
    }

    /**
     * Tries to build the applications from the config file.
     *
     * @return the configuration file, if something wents wrong it returns the
     *         default configurations
     */
    private Configuration buildFromFile() {
        final Configuration.Builder b = new Configuration.Builder();
        try (BufferedReader bf = new BufferedReader(
                new InputStreamReader(
                        Objects.requireNonNull(ClassLoader.getSystemResourceAsStream(FILE_NAME)),
                        StandardCharsets.UTF_8))) {
            for (var line = bf.readLine(); line != null; line = bf.readLine()) {
                final String[] elements = line.split(":");
                if (elements.length == 2) {
                    if (elements[0].contains("max")) {
                        b.withMax(Integer.parseInt(elements[1].trim()));
                    } else if (elements[0].contains("min")) {
                        b.withMin(Integer.parseInt(elements[1].trim()));
                    } else if (elements[0].contains("att")) {
                        b.withAttempts(Integer.parseInt(elements[1].trim()));
                    }
                } else {
                    displayError("problem reading file");
                }
            }
        } catch (final NumberFormatException | IOException e) {
            displayError(e.getMessage());
        }
        return b.build();
    }

    private void displayError(final String err) {
        for (final DrawNumberView view : views) {
            view.displayError(err);
        }
    }

    /**
     * Application entry point.
     *
     * @throws FileNotFoundException if the configuration file cannot be fetched
     */
    public static void main() throws FileNotFoundException {
        new DrawNumberApp(new DrawNumberViewImpl());
    }

}
