package eus.zubirimanteo.misiogalaktikoa.data.repository

import eus.zubirimanteo.misiogalaktikoa.data.dao.MisioDAO
import eus.zubirimanteo.misiogalaktikoa.data.model.Misioa

class MisioRepository(
    private val misioDao: MisioDAO  // Room-eko DAOak injektatzen ditugu
) {
    // Datuak eguneratzeko
    suspend fun eguneratuMisioa(misioa: Misioa) {
        misioDao.eguneratuMisioa(misioa)
    }

    // Datuak ezabatuzeko
    suspend fun ezabatuMisioa(misioa: Misioa) {
        misioDao.ezabatuMisioa(misioa)
    }

    // Datu korrontea zuzenean DAOtik lortzen du (Flow)
    val misioGuztiak = misioDao.getMisioGuztiak()

    // Gehitzeko funtzioa
    suspend fun sortuMisioa(misioa: Misioa) {
        misioDao.sortuMisioa(misioa)
    }
}

/*
Geminiri galdera: Zer da suspend fun?

suspend fun (edo funtzio esekigarria) Kotlin-en korrutinak (coroutines) erabiltzen dituen funtzio berezi bat da. Funtzio hauek "pausatu" eta "berrekin" egin daitezke exekuzioa, hari (thread) nagusia blokeatu gabe.Zergatik da hain garrantzitsua?Android aplikazioetan, interfazearekin (UI) erlazionatutako guztia hari nagusian (main thread) exekutatzen da. Eragiketa luze edo astun bat (adibidez, datu-base batetik datuak irakurtzea, sareko eskaera bat egitea edo fitxategi handi bat prozesatzea) hari horretan exekutatzen baduzu, aplikazioa "izoztu" egingo da. Erabiltzaileak ezingo du botoirik sakatu, ezta scroll egin ere, eragiketa amaitu arte. Hori gertatzen denean, Android-ek "Application Not Responding" (ANR) errorea erakutsi dezake.
 */