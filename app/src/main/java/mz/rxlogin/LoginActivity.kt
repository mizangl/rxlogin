package mz.rxlogin

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Patterns
import com.jakewharton.rxbinding2.widget.RxTextView
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.activity_login.*
import java.util.concurrent.TimeUnit

class LoginActivity : AppCompatActivity() {

  private inline fun retryWhenError(crossinline onError: (ex: Throwable) -> Unit): ObservableTransformer<String, String> = ObservableTransformer { observable ->
    observable.retryWhen { errors ->
      errors.flatMap {
        onError(it)
        Observable.just("")
      }
    }
  }

  private val lengthGraterThantSix = ObservableTransformer<String, String> { observable ->
    observable.flatMap {
      Observable.just(it).map { it.trim() }
          .filter { it.length > 6 }
          .singleOrError()
          .onErrorResumeNext {
            if (it is NoSuchElementException) {
              Single.error(Exception("Length should be greater than 6"))
            } else {
              Single.error(it)
            }
          }
          .toObservable()
    }
  }

  private val verifyEmailPattern = ObservableTransformer<String, String> { observable ->
    observable.flatMap {
      Observable.just(it).map { it.trim() }
          .filter {
            Patterns.EMAIL_ADDRESS.matcher(it).matches()
          }
          .singleOrError()
          .onErrorResumeNext {
            if (it is NoSuchElementException) {
              Single.error(Exception("Email not valid"))
            } else {
              Single.error(it)
            }
          }
          .toObservable()
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_login)

    RxTextView.afterTextChangeEvents(editTextEmail)
        .skipInitialValue()
        .map {
          emailWrapper.error = null
          it.view().text.toString()
        }
        .debounce(1, TimeUnit.SECONDS).observeOn(AndroidSchedulers.mainThread())
        .compose(lengthGraterThantSix)
        .compose(verifyEmailPattern)
        .compose ( retryWhenError { emailWrapper.error = it.message })
        .subscribe()

    RxTextView.afterTextChangeEvents(editTextPassword)
        .skipInitialValue()
        .map {
          passwordWrapper.error = null
          it.view().text.toString()
        }
        .debounce(1, TimeUnit.SECONDS).observeOn(AndroidSchedulers.mainThread())
        .compose(lengthGraterThantSix)
        .compose ( retryWhenError { passwordWrapper.error = it.message })
        .subscribe()
  }
}