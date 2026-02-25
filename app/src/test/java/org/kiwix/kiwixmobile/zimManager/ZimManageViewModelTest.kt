/*
 * Kiwix Android
 * Copyright (c) 2019 Kiwix <android.kiwix.org>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.kiwix.kiwixmobile.zimManager

import android.app.Application
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.TurbineTestContext
import app.cash.turbine.test
import com.jraska.livedata.test
import io.mockk.clearAllMocks
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.RegisterExtension
import org.kiwix.kiwixmobile.core.StorageObserver
import org.kiwix.kiwixmobile.core.dao.LibkiwixBookOnDisk
import org.kiwix.kiwixmobile.core.data.DataSource
import org.kiwix.kiwixmobile.core.downloader.model.DownloadModel
import org.kiwix.kiwixmobile.core.reader.integrity.ValidateZimViewModel
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.files.ScanningProgressListener
import org.kiwix.kiwixmobile.core.zim_manager.NetworkState
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.BooksOnDiskListItem
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.SelectionMode
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.SelectionMode.MULTI
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.SelectionMode.NORMAL
import org.kiwix.kiwixmobile.language.viewmodel.flakyTest
<<<<<<< HEAD
=======
import org.kiwix.sharedFunctions.MainDispatcherRule
import org.kiwix.kiwixmobile.zimManager.Fat32Checker.FileSystemState
import org.kiwix.kiwixmobile.zimManager.Fat32Checker.FileSystemState.CanWrite4GbFile
import org.kiwix.kiwixmobile.zimManager.Fat32Checker.FileSystemState.CannotWrite4GbFile
>>>>>>> 45f2a833d (fix: address PR #4705 review comments and fix CI test failures)
import org.kiwix.kiwixmobile.zimManager.ZimManageViewModel.FileSelectActions.MultiModeFinished
import org.kiwix.kiwixmobile.zimManager.ZimManageViewModel.FileSelectActions.RequestDeleteMultiSelection
import org.kiwix.kiwixmobile.zimManager.ZimManageViewModel.FileSelectActions.RequestMultiSelection
import org.kiwix.kiwixmobile.zimManager.ZimManageViewModel.FileSelectActions.RequestNavigateTo
import org.kiwix.kiwixmobile.zimManager.ZimManageViewModel.FileSelectActions.RequestSelect
<<<<<<< HEAD
=======
import org.kiwix.kiwixmobile.core.compat.CompatHelper.Companion.convertToLocal
>>>>>>> 45f2a833d (fix: address PR #4705 review comments and fix CI test failures)
import org.kiwix.kiwixmobile.zimManager.ZimManageViewModel.FileSelectActions.RequestShareMultiSelection
import org.kiwix.kiwixmobile.zimManager.ZimManageViewModel.FileSelectActions.RequestValidateZimFiles
import org.kiwix.kiwixmobile.zimManager.ZimManageViewModel.FileSelectActions.RestartActionMode
import org.kiwix.kiwixmobile.zimManager.ZimManageViewModel.FileSelectActions.UserClickedDownloadBooksButton
import org.kiwix.kiwixmobile.zimManager.fileselectView.FileSelectListState
import org.kiwix.kiwixmobile.zimManager.fileselectView.effects.DeleteFiles
import org.kiwix.kiwixmobile.zimManager.fileselectView.effects.NavigateToDownloads
import org.kiwix.kiwixmobile.zimManager.fileselectView.effects.None
import org.kiwix.kiwixmobile.zimManager.fileselectView.effects.OpenFileWithNavigation
import org.kiwix.kiwixmobile.zimManager.fileselectView.effects.ShareFiles
import org.kiwix.kiwixmobile.zimManager.fileselectView.effects.StartMultiSelection
import org.kiwix.kiwixmobile.zimManager.fileselectView.effects.ValidateZIMFiles
import org.kiwix.libkiwix.Book
import org.kiwix.sharedFunctions.InstantExecutorExtension
import org.kiwix.sharedFunctions.MainDispatcherRule
import org.kiwix.sharedFunctions.bookOnDisk
import org.kiwix.sharedFunctions.libkiwixBook
import kotlin.time.Duration

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(InstantExecutorExtension::class)
class ZimManageViewModelTest {
  private val libkiwixBookOnDisk: LibkiwixBookOnDisk = mockk()
  private val storageObserver: StorageObserver = mockk()
  private val application: Application = mockk(relaxed = true)
  private val fat32Checker: Fat32Checker = mockk()
  private val dataSource: DataSource = mockk()
  private val alertDialogShower: AlertDialogShower = mockk()
  private val validateZimViewModel: ValidateZimViewModel = mockk()
  lateinit var viewModel: ZimManageViewModel

  private val downloads = MutableStateFlow<List<DownloadModel>>(emptyList())
  private val booksOnFileSystem = MutableStateFlow<List<Book>>(emptyList())
  private val books = MutableStateFlow<List<BooksOnDiskListItem.BookOnDisk>>(emptyList())
  private val onlineContentLanguage = MutableStateFlow("")
  private val fileSystemStates =
    MutableStateFlow<Fat32Checker.FileSystemState>(Fat32Checker.FileSystemState.DetectingFileSystem)
  private val networkStates = MutableStateFlow(NetworkState.NOT_CONNECTED)
  private val booksOnDiskListItems = MutableStateFlow<List<BooksOnDiskListItem>>(emptyList())

  @RegisterExtension
  private val mainDispatcherRule = MainDispatcherRule()
<<<<<<< HEAD
=======
  private val testDispatcher get() = mainDispatcherRule.dispatcher
  private val onlineLibraryManager = mockk<OnlineLibraryManager>(relaxed = true)
  private val onlineLibraryServiceFactory = mockk<OnlineLibraryServiceFactory>()
>>>>>>> 45f2a833d (fix: address PR #4705 review comments and fix CI test failures)

  @AfterEach
  fun teardown() {
    viewModel.onClearedExposed()
  }

  @BeforeEach
  fun init() {
    clearAllMocks()
    every { libkiwixBookOnDisk.books() } returns books
    every {
      storageObserver.getBooksOnFileSystem(
        any<ScanningProgressListener>()
      )
    } returns booksOnFileSystem
    every { fat32Checker.fileSystemStates } returns fileSystemStates
    every { application.getString(any()) } returns ""
    every { application.getString(any(), any()) } returns ""
    every { dataSource.booksOnDiskAsListItems() } returns booksOnDiskListItems
    downloads.value = emptyList()
    booksOnFileSystem.value = emptyList()
    books.value = emptyList()
    fileSystemStates.value = Fat32Checker.FileSystemState.DetectingFileSystem
    booksOnDiskListItems.value = emptyList()
    networkStates.value = NetworkState.NOT_CONNECTED
    onlineContentLanguage.value = ""
    viewModel =
      ZimManageViewModel(
        libkiwixBookOnDisk,
        storageObserver,
        application,
        dataSource,
<<<<<<< HEAD
=======
        connectivityManager,
        onlineLibraryManager,
        kiwixDataStore,
        onlineLibraryServiceFactory,
>>>>>>> 45f2a833d (fix: address PR #4705 review comments and fix CI test failures)
        mainDispatcherRule.dispatcher
      ).apply {
        setAlertDialogShower(alertDialogShower)
        setValidateZimViewModel(validateZimViewModel)
      }
    viewModel.fileSelectListStates.value = FileSelectListState(emptyList())
  }

  @Nested
  inner class Books {
    @Test
    fun `emissions from data source are observed`() = flakyTest {
      runTest {
        val expectedList = listOf(bookOnDisk())
        testFlow(
          viewModel.fileSelectListStates.asFlow(),
          triggerAction = {
            booksOnDiskListItems.emit(expectedList)
            advanceUntilIdle()
          },
          assert = {
            skipItems(1)
            assertThat(awaitItem()).isEqualTo(FileSelectListState(expectedList))
            cancelAndIgnoreRemainingEvents()
          }
        )
      }
    }

    @Test
    fun `books found on filesystem are filtered by books already in db`() = flakyTest {
      runTest {
        every { application.getString(any()) } returns ""
        val expectedBook = bookOnDisk(1L, libkiwixBook("1", nativeBook = BookTestWrapper("1")))
        val bookToRemove = bookOnDisk(1L, libkiwixBook("2", nativeBook = BookTestWrapper("2")))
        advanceUntilIdle()
        books.emit(listOf(bookToRemove))
        booksOnFileSystem.emit(
          listOfNotNull(
            expectedBook.book.nativeBook,
            expectedBook.book.nativeBook,
            bookToRemove.book.nativeBook
          )
        )
        viewModel.requestFileSystemCheck.emit(Unit)
        advanceUntilIdle()
        yield()
        advanceUntilIdle()
        coVerify {
          libkiwixBookOnDisk.insert(listOfNotNull(expectedBook.book.nativeBook))
        }
      }
    }
  }

  @Nested

<<<<<<< HEAD
=======
    @Test
    fun `library section title adapts to selected language count`() = flakyTest {
      runTest(testDispatcher) {
        every { application.getString(R.string.all_languages) } returns "All languages"
        every {
          application.getString(R.string.your_language, any())
        } answers {
          val args = secondArg<Array<Any>>()
          "Selected language: ${args[0]}"
        }
        every { application.getString(R.string.your_languages) } returns "Selected languages:"

        // All languages (blank)
        val allTitle = viewModel.getOnlineLibrarySectionTitle("")
        assertThat(allTitle).isEqualTo("All languages")

        // Single language
        val singleTitle = viewModel.getOnlineLibrarySectionTitle("eng")
        assertThat(singleTitle).contains("Selected language:")
        assertThat(singleTitle).contains("English")

        // Multiple languages
        val multiTitle = viewModel.getOnlineLibrarySectionTitle("eng,fra,deu")
        assertThat(multiTitle).contains("Selected languages:")
        // Locale("eng").displayLanguage returns "English" but
        // Locale("fra") and Locale("deu") may return raw codes on some JVMs
        assertThat(multiTitle).contains("eng".convertToLocal().displayLanguage)
        assertThat(multiTitle).contains("fra".convertToLocal().displayLanguage)
        assertThat(multiTitle).contains("deu".convertToLocal().displayLanguage)
      }
    }
  }

  @Nested
  inner class Categories {
    @Disabled("We will refactor this in migration PR")
    @Test
    fun `changing category updates the filter and do the network request`() = flakyTest {
      runTest(testDispatcher) {
        onlineCategoryContent.value = ""
        every { application.getString(any()) } returns ""
        every { application.getString(any(), any()) } returns ""
        onlineCategoryContent.emit("wikipedia")
        advanceUntilIdle()
        viewModel.onlineLibraryRequest.test {
          skipItems(1)
          onlineCategoryContent.emit("wikipedia")
          var onlineLibraryRequest = awaitItem()
          while (onlineLibraryRequest.category != "wikipedia") onlineLibraryRequest = awaitItem()
          assertThat(onlineLibraryRequest.category).isEqualTo("wikipedia")
          assertThat(onlineLibraryRequest.page).isEqualTo(ONE)
          assertThat(onlineLibraryRequest.isLoadMoreItem).isEqualTo(false)
          cancelAndIgnoreRemainingEvents()
        }
      }
    }
  }

  @Test
  fun `network states observed`() = flakyTest {
    runTest(testDispatcher) {
      networkStates.tryEmit(NOT_CONNECTED)
      advanceUntilIdle()
      viewModel.networkStates.test()
        .assertValue(NOT_CONNECTED)
    }
  }

  @Test
  fun `updateOnlineLibraryFilters updates onlineLibraryRequest`() = flakyTest {
    runTest(testDispatcher) {
      viewModel.setIsUnitTestCase()
      val newRequest = OnlineLibraryRequest(
        query = "test",
        category = "cat",
        lang = "en",
        page = 2,
        isLoadMoreItem = true,
        version = 100L
      )
      viewModel.onlineLibraryRequest.test {
        viewModel.updateOnlineLibraryFilters(newRequest)
        var request = awaitItem()
        while (request != newRequest) request = awaitItem()
        assertThat(request).isEqualTo(newRequest)
      }
    }
  }

  @Test
  fun `library update removes from sources and maps to list items`() = flakyTest {
    runTest(testDispatcher) {
      val book = BookTestWrapper("0")
      val bookAlreadyOnDisk =
        libkiwixBook(id = "0", url = "", language = Locale.ENGLISH.language, nativeBook = book)
      val bookDownloading = libkiwixBook(id = "1", url = "")
      val bookWithActiveLanguage = libkiwixBook(id = "3", language = "activeLanguage", url = "")
      viewModel.libraryItems.test {
        every { application.getString(any()) } returns ""
        every { application.getString(any(), any()) } returns ""
        coEvery {
          onlineLibraryManager.parseOPDSStreamAndGetBooks(any(), any())
        } returns arrayListOf(bookWithActiveLanguage)
        networkStates.value = CONNECTED
        downloads.value = listOf(downloadModel(book = bookDownloading))
        books.value = listOf(bookOnDisk(book = bookAlreadyOnDisk))
        fileSystemStates.value = CanWrite4GbFile
        advanceUntilIdle()

        val items = awaitItem()
        val bookItems = items.items.filterIsInstance<LibraryListItem.BookItem>()
        if (bookItems.size >= 2 && bookItems[0].fileSystemState == CanWrite4GbFile) {
          assertThat(items.items).isEqualTo(
            listOf(
              LibraryListItem.DividerItem(Long.MAX_VALUE, "Downloading:"),
              LibraryListItem.LibraryDownloadItem(downloadModel(book = bookDownloading)),
              LibraryListItem.DividerItem(Long.MAX_VALUE - 1, ""),
              LibraryListItem.BookItem(bookWithActiveLanguage, CanWrite4GbFile),
            )
          )
        }
        cancelAndIgnoreRemainingEvents()
      }
    }
  }

  @Disabled("We will refactor this in migration PR")
  @Test
  fun `library marks files over 4GB as can't download if file system state says to`() = flakyTest {
    runTest(testDispatcher) {
      onlineContentLanguage.value = ""
      val bookOver4Gb =
        libkiwixBook(
          id = "0",
          url = "",
          size = "${Fat32Checker.FOUR_GIGABYTES_IN_KILOBYTES + 1}"
        )
      every { application.getString(any()) } returns "All languages"
      every { application.getString(any(), any()) } returns "All languages"
      every { application.getString(any(), *anyVararg()) } returns "All languages"
      coEvery { kiwixDataStore.selectedOnlineContentLanguage } returns flowOf("")
      // test libraryItems fetches for all language.
      viewModel.libraryItems.test {
        coEvery {
          onlineLibraryManager.parseOPDSStreamAndGetBooks(any(), any())
        } returns arrayListOf(bookOver4Gb)
        networkStates.value = CONNECTED
        downloads.value = listOf()
        books.value = listOf()
        onlineContentLanguage.value = ""
        fileSystemStates.emit(FileSystemState.DetectingFileSystem)
        fileSystemStates.emit(CannotWrite4GbFile)
        advanceUntilIdle()

        awaitItem()
        val item = awaitItem()
        val bookItem = item.items.filterIsInstance<LibraryListItem.BookItem>().firstOrNull()
        if (bookItem?.fileSystemState == CannotWrite4GbFile) {
          assertThat(item.items).isEqualTo(
            listOf(
              LibraryListItem.DividerItem(Long.MIN_VALUE, "All languages"),
              LibraryListItem.BookItem(bookOver4Gb, CannotWrite4GbFile)
            )
          )
        }
        cancelAndConsumeRemainingEvents()
      }

      // test library items fetches for a particular language
      viewModel.libraryItems.test {
        coEvery {
          onlineLibraryManager.parseOPDSStreamAndGetBooks(any(), any())
        } returns arrayListOf(bookOver4Gb)
        every { application.getString(any(), any()) } returns "Selected language: English"
        every { application.getString(any(), *anyVararg()) } returns "Selected language: English"
        networkStates.value = CONNECTED
        downloads.value = listOf()
        books.value = listOf()
        onlineContentLanguage.value = "eng"
        fileSystemStates.emit(FileSystemState.DetectingFileSystem)
        fileSystemStates.emit(CannotWrite4GbFile)
        advanceUntilIdle()

        val item = awaitItem()
        val bookItem = item.items.filterIsInstance<LibraryListItem.BookItem>().firstOrNull()
        if (bookItem?.fileSystemState == CannotWrite4GbFile) {
          assertThat(item.items).isEqualTo(
            listOf(
              LibraryListItem.DividerItem(Long.MIN_VALUE, "Selected language: English"),
              LibraryListItem.BookItem(bookOver4Gb, CannotWrite4GbFile)
            )
          )
        }
        cancelAndConsumeRemainingEvents()
      }
    }

    @Test
    fun `library shows selected language section title correctly`() = flakyTest {
      runTest(testDispatcher) {
        val bookOver4Gb =
          libkiwixBook(
            id = "0",
            url = "",
            size = "${Fat32Checker.FOUR_GIGABYTES_IN_KILOBYTES + 1}"
          )
        every { application.getString(any()) } answers { "" }
        every { application.getString(any(), any()) } answers { "" }
        every { application.getString(any(), *anyVararg()) } answers { "Selected language: English" }

        // test libraryItems fetches for all language.
        viewModel.libraryItems.test {
          coEvery {
            onlineLibraryManager.parseOPDSStreamAndGetBooks(any(), any())
          } returns arrayListOf(bookOver4Gb)
          networkStates.value = CONNECTED
          downloads.value = listOf()
          books.value = listOf()
          onlineContentLanguage.value = "eng"
          yield()
          fileSystemStates.emit(FileSystemState.DetectingFileSystem)
          fileSystemStates.emit(CannotWrite4GbFile)
          advanceUntilIdle()

          var matched = false
          while (!matched) {
            val item = awaitItem()
            val bookItem = item.items.filterIsInstance<LibraryListItem.BookItem>().firstOrNull()
            if (bookItem?.fileSystemState == CannotWrite4GbFile) {
              assertThat(item.items).isEqualTo(
                listOf(
                  LibraryListItem.DividerItem(Long.MIN_VALUE, "Selected language: English"),
                  LibraryListItem.BookItem(bookOver4Gb, CannotWrite4GbFile)
                )
              )
              matched = true
            }
          }
          cancelAndConsumeRemainingEvents()
        }
      }
    }
  }

  @Test
  fun `library shows downloading books even when not in online source`() = flakyTest {
    runTest(testDispatcher) {
      val downloadingBook = libkiwixBook(id = "10", url = "")
      val bookInOnlineList = libkiwixBook(id = "20", url = "")
      val downloadModel = downloadModel(book = downloadingBook)

      every { application.getString(any()) } returns "Downloading"
      every { application.getString(any(), any()) } returns "All languages"
      every { application.getString(any(), *anyVararg()) } returns "All languages"

      viewModel.libraryItems.test {
        coEvery {
          onlineLibraryManager.parseOPDSStreamAndGetBooks(any(), any())
        } returns arrayListOf(bookInOnlineList)
        networkStates.value = CONNECTED
        downloads.value = listOf(downloadModel)
        books.value = listOf()
        onlineContentLanguage.value = ""
        fileSystemStates.value = CanWrite4GbFile
        advanceUntilIdle()

        val items = awaitItem()
        val bookItems = items.items.filterIsInstance<LibraryListItem.BookItem>()
        if (bookItems.size >= 2) {
          assertThat(items.items).isEqualTo(
            listOf(
              LibraryListItem.DividerItem(Long.MAX_VALUE, "Downloading"),
              LibraryListItem.LibraryDownloadItem(downloadModel),
              LibraryListItem.DividerItem(Long.MIN_VALUE, "All languages"),
              LibraryListItem.BookItem(bookInOnlineList, CanWrite4GbFile)
            )
          )
        }
        cancelAndIgnoreRemainingEvents()
      }
    }
  }

  @Nested
>>>>>>> 45f2a833d (fix: address PR #4705 review comments and fix CI test failures)
  inner class SideEffects {
    @Test
    fun `RequestNavigateTo offers OpenFileWithNavigation with selected books`() = flakyTest {
      runTest {
        val selectedBook = bookOnDisk(isSelected = true)
        viewModel.fileSelectListStates.value =
          FileSelectListState(listOf(selectedBook, bookOnDisk()), NORMAL)
        testFlow(
          flow = viewModel.sideEffects,
          triggerAction = {
            viewModel.fileSelectActions.emit(RequestNavigateTo(selectedBook))
          },
          assert = { assertThat(awaitItem()).isEqualTo(OpenFileWithNavigation(selectedBook)) }
        )
      }
    }

    @Disabled(
      "Temporarily skipping this test." +
        " We are migrating OnlineLibrary and LocalLibrary into separate ones." +
        " There we will fix this test"
    )
    @Test
    fun `RequestMultiSelection offers StartMultiSelection and selects a book`() = flakyTest {
      runTest {
        val bookToSelect = bookOnDisk(databaseId = 0L)
        val unSelectedBook = bookOnDisk(databaseId = 1L)
        viewModel.fileSelectListStates.value =
          FileSelectListState(
            listOf(
              bookToSelect,
              unSelectedBook
            ),
            SelectionMode.NORMAL
          )
        testFlow(
          flow = viewModel.sideEffects,
          triggerAction = { viewModel.fileSelectActions.emit(RequestMultiSelection(bookToSelect)) },
          assert = { assertThat(awaitItem()).isEqualTo(StartMultiSelection(viewModel.fileSelectActions)) }
        )
        viewModel.fileSelectListStates.test()
          .assertValue(
            FileSelectListState(
              listOf(bookToSelect.copy(isSelected = !bookToSelect.isSelected), unSelectedBook),
              MULTI
            )
          )
      }
    }

    @Test
    fun `RequestDeleteMultiSelection offers DeleteFiles with selected books`() = flakyTest {
      runTest {
        val selectedBook = bookOnDisk(isSelected = true)
        viewModel.fileSelectListStates.value =
          FileSelectListState(listOf(selectedBook, bookOnDisk()), NORMAL)
        testFlow(
          flow = viewModel.sideEffects,
          triggerAction = { viewModel.fileSelectActions.emit(RequestDeleteMultiSelection) },
          assert = {
            assertThat(awaitItem()).isEqualTo(
              DeleteFiles(
                listOf(selectedBook),
                alertDialogShower
              )
            )
          }
        )
      }
    }

    @Test
    fun `RequestShareMultiSelection offers ShareFiles with selected books`() = flakyTest {
      runTest {
        val selectedBook = bookOnDisk(isSelected = true)
        viewModel.fileSelectListStates.value =
          FileSelectListState(listOf(selectedBook, bookOnDisk()), NORMAL)
        testFlow(
          flow = viewModel.sideEffects,
          triggerAction = { viewModel.fileSelectActions.emit(RequestShareMultiSelection) },
          assert = {
            assertThat(awaitItem()).isEqualTo(
              ShareFiles(
                listOf(selectedBook),
                viewModel.viewModelScope,
                mainDispatcherRule.dispatcher
              )
            )
          }
        )
      }
    }

    @Test
    fun `RequestValidateZimFiles offers ValidateZIMFiles with selected books`() = flakyTest {
      runTest {
        val selectedBook = bookOnDisk(isSelected = true)
        viewModel.fileSelectListStates.value =
          FileSelectListState(listOf(selectedBook, bookOnDisk()), NORMAL)
        testFlow(
          flow = viewModel.sideEffects,
          triggerAction = { viewModel.fileSelectActions.emit(RequestValidateZimFiles) },
          assert = {
            assertThat(awaitItem())
              .isEqualTo(
                ValidateZIMFiles(
                  listOf(selectedBook),
                  alertDialogShower,
                  validateZimViewModel
                )
              )
          }
        )
      }
    }

    @Test
    fun `MultiModeFinished offers None`() = flakyTest {
      runTest {
        val selectedBook = bookOnDisk(isSelected = true)
        viewModel.fileSelectListStates.value =
          FileSelectListState(listOf(selectedBook, bookOnDisk()), NORMAL)
        testFlow(
          flow = viewModel.sideEffects,
          triggerAction = { viewModel.fileSelectActions.emit(MultiModeFinished) },
          assert = { assertThat(awaitItem()).isEqualTo(None) }
        )
        viewModel.fileSelectListStates.test().assertValue(
          FileSelectListState(
            listOf(
              selectedBook.copy(isSelected = false),
              bookOnDisk()
            )
          )
        )
      }
    }

    @Disabled(
      "Temporarily skipping this test." +
        " We are migrating OnlineLibrary and LocalLibrary into separate ones." +
        " There we will fix this test"
    )
    @Test
    fun `RequestSelect offers None and inverts selection`() = flakyTest {
      runTest {
        val selectedBook = bookOnDisk(0L, isSelected = true)
        val unselectedBook = bookOnDisk(1L)
        viewModel.fileSelectListStates.value =
          FileSelectListState(listOf(selectedBook, unselectedBook), NORMAL)
        testFlow(
          flow = viewModel.sideEffects,
          triggerAction = { viewModel.fileSelectActions.emit(RequestSelect(selectedBook)) },
          assert = { assertThat(awaitItem()).isEqualTo(None) }
        )
        viewModel.fileSelectListStates.test().assertValue(
          FileSelectListState(
            listOf(
              selectedBook.copy(isSelected = false),
              unselectedBook.copy(isSelected = false)
            )
          )
        )
      }
    }

    @Test
    fun `RestartActionMode offers StartMultiSelection`() = flakyTest {
      runTest {
        testFlow(
          flow = viewModel.sideEffects,
          triggerAction = { viewModel.fileSelectActions.emit(RestartActionMode) },
          assert = { assertThat(awaitItem()).isEqualTo(StartMultiSelection(viewModel.fileSelectActions)) }
        )
      }
    }

    @Test
    fun `UserClickedDownloadBooksButton offers NavigateToDownloads`() = flakyTest {
      runTest {
        testFlow(
          flow = viewModel.sideEffects,
          triggerAction = { viewModel.fileSelectActions.emit(UserClickedDownloadBooksButton) },
          assert = { assertThat(awaitItem()).isEqualTo(NavigateToDownloads) }
        )
      }
    }
  }
}

suspend fun <T> TestScope.testFlow(
  flow: Flow<T>,
  triggerAction: suspend () -> Unit,
  assert: suspend TurbineTestContext<T>.() -> Unit,
  timeout: Duration? = null
) {
  flow.test(timeout = timeout) {
    triggerAction()
    assert()
    cancelAndIgnoreRemainingEvents()
  }
}

suspend inline fun <reified T> ReceiveTurbine<*>.awaitItemOfType(): T {
  while (true) {
    val item = awaitItem()
    if (item is T) return item
  }
}

class BookTestWrapper(private val id: String) : Book(0L) {
  override fun getId(): String = id
  override fun equals(other: Any?): Boolean = other is BookTestWrapper && getId() == other.getId()
  override fun hashCode(): Int = getId().hashCode()
}
<<<<<<< HEAD
=======

const val MOCKK_TIMEOUT_FOR_VERIFICATION = 1000L

private class TestZimManageViewModel(
  downloadDao: DownloadRoomDao,
  libkiwixBookOnDisk: LibkiwixBookOnDisk,
  storageObserver: StorageObserver,
  kiwixService: KiwixService,
  context: Application,
  connectivityBroadcastReceiver: ConnectivityBroadcastReceiver,
  fat32Checker: Fat32Checker,
  dataSource: DataSource,
  connectivityManager: ConnectivityManager,
  onlineLibraryManager: OnlineLibraryManager,
  kiwixDataStore: KiwixDataStore,
  onlineLibraryServiceFactory: OnlineLibraryServiceFactory,
  ioDispatcher: CoroutineDispatcher
) : ZimManageViewModel(
    downloadDao,
    libkiwixBookOnDisk,
    storageObserver,
    kiwixService,
    context,
    connectivityBroadcastReceiver,
    fat32Checker,
    dataSource,
    connectivityManager,
    onlineLibraryManager,
    kiwixDataStore,
    onlineLibraryServiceFactory,
    ioDispatcher
  )

>>>>>>> 45f2a833d (fix: address PR #4705 review comments and fix CI test failures)
