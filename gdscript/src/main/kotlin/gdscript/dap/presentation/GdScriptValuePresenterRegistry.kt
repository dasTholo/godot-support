package gdscript.dap.presentation

class GdScriptValuePresenterRegistry(private val presenters: List<GdScriptValuePresenter>) {
    fun find(type: String): GdScriptValuePresenter? =
        presenters.firstOrNull { it.canPresent(type) }
}
