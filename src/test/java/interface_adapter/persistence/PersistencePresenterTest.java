package interface_adapter.persistence;

import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.List;

import entity.Ticker;
import entity.Watchlist;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Covers the persistence presenter and its view model together, because the pair is only
 * meaningful as a pair: the presenter's entire job is to turn a use-case callback into a
 * status string on the view model, and the view model's entire job is to announce it.
 *
 * <p>These are the two classes {@code app.Main} binds to the status line in {@code MainView}.
 * Until that binding existed a failed save still showed the success message, so the assertions
 * that matter most here are the ones proving a failure produces a <em>different</em> message and
 * that every state change actually fires a notification.
 */
final class PersistencePresenterTest {

    /** Records every property change the view model fires, in order. */
    private static List<PropertyChangeEvent> listen(PersistenceViewModel viewModel) {
        final List<PropertyChangeEvent> events = new ArrayList<>();
        viewModel.addPropertyChangeListener(events::add);
        return events;
    }

    @Test
    void newViewModelStartsBlank() {
        final PersistenceViewModel viewModel = new PersistenceViewModel();

        assertEquals("", viewModel.getStatusMessage());
    }

    @Test
    void successfulSaveAnnouncesSavedMessage() {
        final PersistenceViewModel viewModel = new PersistenceViewModel();
        final List<PropertyChangeEvent> events = listen(viewModel);
        final PersistencePresenter presenter = new PersistencePresenter(viewModel);

        presenter.prepareSuccessView();

        assertEquals("Watchlist saved.", viewModel.getStatusMessage());
        assertEquals(1, events.size());
        assertEquals(PersistenceViewModel.STATUS_PROPERTY, events.get(0).getPropertyName());
        assertEquals("Watchlist saved.", events.get(0).getNewValue());
    }

    @Test
    void aLoadIsAnnouncedWithoutTheWatchlistItselfReachingTheViewModel() {
        final PersistenceViewModel viewModel = new PersistenceViewModel();
        final List<PropertyChangeEvent> events = listen(viewModel);
        final PersistencePresenter presenter = new PersistencePresenter(viewModel);

        final Watchlist loaded = new Watchlist();
        loaded.addTicker(new Ticker("AAPL", "Apple Inc."));

        presenter.presentWatchlist(loaded);

        // The watchlist stops at the presenter. It used to be parked on the view model for
        // Main to read back out, which put an entity within reach of every screen holding
        // that view model; Main takes it from the load boundary instead. All the view model
        // learns is the sentence.
        assertEquals("Watchlist loaded.", viewModel.getStatusMessage());
        assertEquals(1, events.size());
        assertEquals("Watchlist loaded.", events.get(0).getNewValue());
    }

    @Test
    void failureAnnouncesTheUseCaseMessageRatherThanASuccessString() {
        final PersistenceViewModel viewModel = new PersistenceViewModel();
        final List<PropertyChangeEvent> events = listen(viewModel);
        final PersistencePresenter presenter = new PersistencePresenter(viewModel);

        presenter.prepareFailView("Could not save your watchlist: disk full.");

        assertEquals("Could not save your watchlist: disk full.", viewModel.getStatusMessage());
        assertEquals(1, events.size());
        assertEquals(PersistenceViewModel.STATUS_PROPERTY, events.get(0).getPropertyName());
    }

    @Test
    void aFailureAfterASuccessReplacesTheMessage() {
        final PersistenceViewModel viewModel = new PersistenceViewModel();
        final PersistencePresenter presenter = new PersistencePresenter(viewModel);
        final List<PropertyChangeEvent> events = listen(viewModel);

        presenter.prepareSuccessView();
        presenter.prepareFailView("Could not save your watchlist: permission denied.");

        // The regression this guards: a save that fails after one that succeeded must not
        // leave "Watchlist saved." on screen.
        assertEquals("Could not save your watchlist: permission denied.", viewModel.getStatusMessage());
        assertEquals(2, events.size());
    }

    @Test
    void setStatusMessageNotifiesEvenWhenTheMessageRepeats() {
        final PersistenceViewModel viewModel = new PersistenceViewModel();
        final List<PropertyChangeEvent> events = listen(viewModel);

        viewModel.setStatusMessage("Watchlist saved.");
        viewModel.setStatusMessage("Watchlist saved.");

        // The view model fires with a null old value precisely so PropertyChangeSupport cannot
        // suppress a repeat as "no change" - two consecutive saves must both be announced.
        assertEquals(2, events.size());
        assertNull(events.get(1).getOldValue());
    }
}
