package jo.celeris.demo.exemple

import java.util.Map
import kotlin.math.sqrt

/**
 * Corpus Kotlin d'exemple — exerce les constructions que l'analyseur
 * doit couvrir : fonctions, classes de données, objets compagnons,
 * gabarits de chaînes, chaînes brutes, when, annotations et
 * documentation imbriquée.
 */
data class Point(val x: Double, val y: Double) {
    fun norme(): Double {
        return sqrt(x * x + y * y)
    }
}

class Catalogue(prive: Map<String, List<Int>>) {
    companion object {
        const val LIMITE = 512
    }

    private val index = prive
    private var compteur = 0

    /** Additionne les valeurs de la fenêtre, gabarit compris. */
    fun additionne(valeurs: List<Int>): Int {
        var somme = 0
        for (v in valeurs) {
            if (somme > LIMITE) break
            somme += v
        }
        return "somme = ${somme}".length
    }

    fun decrit(cle: String): String {
        val brute = """
            la cle $cle porte
            plusieurs lignes brutes
        """
        return brute.trim()
    }

    fun etat(v: Int): String {
        return when {
            v < 0 -> "negatif"
            v == 0 -> "nul"
            else -> "positif"
        }
    }
}

object Utilitaires {
    val salutation: String = "Bienvenue"

    fun salue(nom: String): String {
        return "$salutation, $nom !"
    }
}

val SEUIL_PUBLIC = 42

fun reindexe(catalogue: Catalogue): Int {
    return SEUIL_PUBLIC + 1
}
