// Corpus C++ — le tampon de lignes d'un éditeur embarquable.
#include <cstdint>
#include <string>
#include <vector>

namespace celeris {

constexpr std::size_t TAILLE_TAMPON = 4096;

class TamponLignes {
public:
    explicit TamponLignes(std::string texte)
        : texte_(std::move(texte)) {
        indexe();
    }

    virtual ~TamponLignes() = default;

    std::size_t lignes() const noexcept { return debuts_.size(); }

    const std::string &texte() const noexcept { return texte_; }

    // Le début de la ligne demandée, 0 si hors bornes.
    std::size_t debut(std::size_t ligne) const {
        if (ligne >= debuts_.size()) {
            return 0;
        }
        return debuts_[ligne];
    }

protected:
    virtual void indexe() {
        debuts_.clear();
        debuts_.push_back(0);
        for (std::size_t i = 0; i < texte_.size(); ++i) {
            if (texte_[i] == '\n') {
                debuts_.push_back(i + 1);
            }
        }
    }

private:
    std::string texte_;
    std::vector<std::size_t> debuts_;
};

template <typename T>
T borne(const T &valeur, const T &minimum, const T &maximum) {
    if (valeur < minimum) {
        return minimum;
    }
    return valeur > maximum ? maximum : valeur;
}

}  // namespace celeris

int main() {
    using celeris::TamponLignes;
    TamponLignes tampon("une\ndeux\n");
    auto lignes = static_cast<int>(tampon.lignes());
    auto taille = borne(lignes, 0, 10);
    return taille == 3 ? 0 : 1;
}
