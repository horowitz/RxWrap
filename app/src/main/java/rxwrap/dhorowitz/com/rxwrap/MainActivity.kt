package rxwrap.dhorowitz.com.rxwrap

import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Single

@SuppressLint("MissingPermission")
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        getLocation()

        getLocationRx()
                .flatMap{ location -> getNearbyPlaces(location) }
                .subscribe { places ->
                    Log.d(TAG,"{${places.size}}Places available")
                }
    }


    fun getLocation() {
        val fusedClient = LocationServices.getFusedLocationProviderClient(this)
        fusedClient.lastLocation.addOnSuccessListener { location ->
            Log.d(TAG, "user location ${location.latitude},${location.longitude}")
        }.addOnFailureListener {
            Log.e(TAG,"error fetching location",it)
        }
    }

    private fun getLocationRx(): Single<Location> {
        val fusedClient = LocationServices.getFusedLocationProviderClient(this)
        return Single.create<Location> { emitter ->
            fusedClient.lastLocation.addOnSuccessListener { location ->
                Log.d(TAG, "user location ${location.latitude},${location.longitude}")
                emitter.onSuccess(location)
            }.addOnFailureListener {error ->
                Log.e(TAG,"error fetching location",error)
                emitter.onError(error)
            }
        }
    }

    private fun getNearbyPlaces(location: Location): Single<List<Place>> {


        Log.d(TAG,"" + location)
        return Single.just(listOf())
    }



    fun getDocument() {
        val db = FirebaseFirestore.getInstance()
        val docRef = db.collection("collection").document("document")
        docRef.get().addOnCompleteListener(OnCompleteListener<DocumentSnapshot> { task ->
            if (task.isSuccessful) {
                val document = task.result
                if (document.exists()) {
                    Log.d(TAG, "DocumentSnapshot data: " + document.data!!)
                } else {
                    Log.d(TAG, "No such document")
                }
            } else {
                Log.d(TAG, "get failed with ", task.exception)
            }
        })
    }

    fun getDocumentRx(): Single<DocumentSnapshot> {
        val db = FirebaseFirestore.getInstance()
        val docRef = db.collection("collection").document("document")
        return Single.create<DocumentSnapshot> { emitter ->
            docRef.get().addOnCompleteListener({ task ->
                if (task.isSuccessful) {
                    val document = task.result
                    if (document.exists()) {
                        emitter.onSuccess(document)
                    } else {
                        val message = "No such document"
                        Log.d(TAG, message)
                        emitter.onError(Exception(message))
                    }
                } else {
                    Log.d(TAG, "get failed with ", task.exception)
                    emitter.onError(Exception("an error occurred"))
                }
            })
        }
    }

    fun getDocumentRxWithBackPressure(): Flowable<DocumentSnapshot> {
        val db = FirebaseFirestore.getInstance()
        val docRef = db.collection("collection").document("document")
        return Flowable.create<DocumentSnapshot>({ emitter ->
            docRef.get().addOnCompleteListener({ task ->
                if (task.isSuccessful) {
                    val document = task.result
                    if (document.exists()) {
                        emitter.onNext(document)
                    } else {
                        val message = "No such document"
                        Log.d(TAG, message)
                        emitter.onError(Exception(message))
                    }
                } else {
                    Log.d(TAG, "get failed with ", task.exception)
                    emitter.onError(Exception("an error occurred"))
                }
            })
        }, BackpressureStrategy.BUFFER)
    }

    companion object {
        val TAG = MainActivity::class.java.simpleName
    }

    data class Place(val id: String){}
}
