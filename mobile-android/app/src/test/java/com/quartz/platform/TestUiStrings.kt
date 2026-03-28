package com.quartz.platform

import com.quartz.platform.core.text.UiStrings

class TestUiStrings : UiStrings {
    override fun get(resId: Int, vararg formatArgs: Any): String {
        val template = when (resId) {
            R.string.info_demo_snapshot_loaded -> "Snapshot de démo chargé. Vous pouvez maintenant ouvrir le détail d'un site."
            R.string.error_load_demo_snapshot -> "Impossible de charger le snapshot de démo."
            R.string.error_load_cached_sites -> "Impossible de charger les sites en cache."
            R.string.info_user_location_unavailable -> "Position utilisateur indisponible, recentrage sur le site sélectionné."
            R.string.info_user_location_unavailable_no_site -> "Position utilisateur indisponible."
            R.string.info_local_draft_created -> "Brouillon local créé."
            R.string.error_create_local_draft -> "Impossible de créer un brouillon local."
            R.string.error_load_site_detail -> "Impossible de charger le détail du site."
            R.string.error_report_title_empty -> "Le titre du rapport ne peut pas être vide."
            R.string.error_draft_not_found_during_update -> "Brouillon introuvable pendant la mise à jour."
            R.string.error_save_local_draft -> "Impossible d'enregistrer le brouillon local."
            R.string.info_local_draft_saved -> "Brouillon enregistré localement."
            R.string.error_save_before_sync -> "Enregistrez le brouillon avant de lancer la synchronisation."
            R.string.error_enqueue_report_sync -> "Impossible d'ajouter le brouillon à la file de synchronisation."
            R.string.info_enqueued_report_sync -> "Brouillon ajouté à la file de synchronisation."
            R.string.info_debug_mode_updated -> "Mode debug de synchronisation mis à jour: %1\$s"
            R.string.error_observe_local_draft -> "Impossible de charger le brouillon local."
            R.string.error_observe_sync_state -> "Impossible de suivre l'état de synchronisation."
            R.string.error_observe_debug_mode -> "Impossible de suivre le mode de simulation debug."
            R.string.info_sync_relaunched -> "Synchronisation relancée pour ce rapport."
            R.string.info_retry_only_failed -> "Relance autorisée uniquement pour les rapports en échec."
            R.string.error_retry_sync -> "Impossible de relancer la synchronisation."
            R.string.error_load_local_reports -> "Impossible de charger les rapports locaux."
            R.string.error_xfeeder_observe_session -> "Impossible de charger la session guidée secteur."
            R.string.error_xfeeder_sector_not_found -> "Secteur introuvable dans le snapshot technique local."
            R.string.error_xfeeder_create_session -> "Impossible de créer la session guidée locale."
            R.string.error_xfeeder_update_step -> "Impossible de mettre à jour l'étape guidée."
            R.string.error_xfeeder_save_summary -> "Impossible d'enregistrer le résumé guidé."
            R.string.error_xfeeder_complete_requires_required_steps -> "Impossible de clôturer la session: terminez toutes les étapes obligatoires."
            R.string.error_xfeeder_closure_requires_related_sector -> "Impossible de clôturer: renseignez le secteur lié pour ce résultat."
            R.string.error_xfeeder_closure_requires_unreliable_reason -> "Impossible de clôturer: sélectionnez le motif mesures non fiables."
            R.string.error_xfeeder_closure_requires_observed_sector_count -> "Impossible de clôturer: indiquez un nombre de secteurs observés supérieur ou égal à 2."
            R.string.info_xfeeder_session_created -> "Session guidée locale créée."
            R.string.info_xfeeder_summary_saved -> "Session guidée enregistrée localement."
            R.string.error_ret_observe_session -> "Impossible de charger la session RET secteur."
            R.string.error_ret_sector_not_found -> "Secteur introuvable dans le snapshot technique local pour RET."
            R.string.error_ret_create_session -> "Impossible de créer la session RET locale."
            R.string.error_ret_update_step -> "Impossible de mettre à jour l'étape RET."
            R.string.error_ret_save_summary -> "Impossible d'enregistrer le résumé RET."
            R.string.error_ret_create_report_draft -> "Impossible de créer le brouillon rapport depuis la session RET."
            R.string.error_ret_complete_requires_required_steps -> "Impossible de clôturer la session RET: terminez toutes les étapes obligatoires."
            R.string.info_ret_session_created -> "Session RET locale créée."
            R.string.info_ret_summary_saved -> "Session RET enregistrée localement."
            R.string.info_ret_opened_linked_draft -> "Brouillon déjà lié à cette session RET: ouverture du brouillon existant."
            else -> "res:$resId"
        }

        return if (formatArgs.isEmpty()) {
            template
        } else {
            String.format(template, *formatArgs)
        }
    }
}
