package com.juriconstat.config;

import com.juriconstat.model.User;
import com.juriconstat.model.Consultation;
import com.juriconstat.repository.UserRepository;
import com.juriconstat.repository.ConsultationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ConsultationRepository consultationRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        log.info("Checking if database seeding is required...");

        // Seed demo user: Choussi Akuta (choussi69@gmail.com)
        if (!userRepository.existsByEmail("choussi69@gmail.com")) {
            log.info("Seeding demo user Choussi Akuta...");
            User choussi = User.builder()
                    .nom("Choussi Akuta")
                    .email("choussi69@gmail.com")
                    .password(passwordEncoder.encode("password"))
                    .pays("CM")
                    .langue("fr")
                    .role("USER")
                    .abonnement("PREMIUM")
                    .build();
            userRepository.save(choussi);

            // Seed mock consultations for the demo list history
            log.info("Seeding past consultations for Choussi Akuta...");
            
            Consultation c1 = Consultation.builder()
                    .user(choussi)
                    .requete("Quels sont mes droits si j'ai un accident de voiture et le responsable s'enfuit ?")
                    .reponseIa("En cas d'accident avec délit de fuite, vous devez immédiatement :\n\n1. **Porter plainte** au commissariat le plus proche en précisant tout élément d'identification (marque, couleur, plaque, témoins).\n2. **Faire constater vos blessures** par un médecin et obtenir un certificat médical initial.\n3. **Saisir le Fonds de Garantie des Assurances Obligatoires (FGAO)** qui indemnise les victimes corporelles lorsque le responsable est inconnu.\n\nConservez bien tous les justificatifs de frais médicaux et de réparation automobile pour votre dossier de réclamation.")
                    .pays("CM")
                    .langue("fr")
                    .createdAt(LocalDateTime.now().minusHours(2))
                    .build();

            Consultation c2 = Consultation.builder()
                    .user(choussi)
                    .requete("Qu'est-ce que le harcèlement au travail et comment le prouver ?")
                    .reponseIa("Le harcèlement moral au travail se définit par des agissements répétés ayant pour objet ou pour effet une dégradation des conditions de travail susceptible de porter atteinte aux droits et à la dignité du salarié, d'altérer sa santé ou de compromettre son avenir professionnel.\n\nPour le prouver, vous devez réunir :\n- **Des preuves écrites** : courriels, SMS, notes de service, évaluations injustifiées.\n- **Des témoignages** : attestations de collègues ou de clients.\n- **Des éléments médicaux** : arrêts de travail, certificats du médecin du travail.\n\nVous pouvez alerter le Comité Social et Économique (CSE) ou l'inspection du travail.")
                    .pays("CM")
                    .langue("fr")
                    .createdAt(LocalDateTime.now().minusDays(1))
                    .build();

            Consultation c3 = Consultation.builder()
                    .user(choussi)
                    .requete("Quelles sont les clauses abusives fréquentes dans un bail commercial ?")
                    .reponseIa("Dans un bail commercial, certaines clauses sont fréquemment jugées abusives ou réputées non écrites car elles déséquilibrent excessivement le contrat :\n\n1. **La clause de solidarité inversée sans limite** : qui oblige le cédant à garantir le loyer indéfiniment après la cession.\n2. **L'imposition de gros travaux (Article 606 du Code Civil)** : la loi Pinel interdit de faire supporter au locataire les grosses réparations touchant à la structure de l'immeuble.\n3. **La clause de réévaluation unilatérale** : qui contourne les indices légaux trimestriels (ILC ou ILAT).\n\nIl est recommandé de faire auditer le projet de bail avant signature.")
                    .pays("CM")
                    .langue("fr")
                    .createdAt(LocalDateTime.now().minusDays(3))
                    .build();

            consultationRepository.save(c1);
            consultationRepository.save(c2);
            consultationRepository.save(c3);

            log.info("Database successfully seeded with demo baseline data!");
        } else {
            log.info("Database already seeded. Skipping.");
        }
    }
}
