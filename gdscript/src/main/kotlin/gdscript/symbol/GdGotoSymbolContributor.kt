package gdscript.symbol

import com.intellij.navigation.ChooseByNameContributorEx
import com.intellij.navigation.NavigationItem
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StubIndex
import com.intellij.util.Processor
import com.intellij.util.indexing.FindSymbolParameters
import com.intellij.util.indexing.IdFilter
import common.index.StringStubIndexExtensionExt
import gdscript.index.impl.GdClassDeclIndex
import gdscript.index.impl.GdClassVarDeclIndex
import gdscript.index.impl.GdConstDeclIndex
import gdscript.index.impl.GdEnumDeclIndex
import gdscript.index.impl.GdMethodDeclIndex
import gdscript.index.impl.GdSignalDeclIndex

class GdGotoSymbolContributor : ChooseByNameContributorEx {

    private val indices: List<StringStubIndexExtensionExt<*>> = listOf(
        GdMethodDeclIndex.INSTANCE,
        GdClassVarDeclIndex.INSTANCE,
        GdConstDeclIndex.INSTANCE,
        GdEnumDeclIndex.INSTANCE,
        GdSignalDeclIndex.INSTANCE,
        GdClassDeclIndex.INSTANCE,
    )

    override fun processNames(
        processor: Processor<in String>,
        scope: GlobalSearchScope,
        filter: IdFilter?,
    ) {
        for (index in indices) {
            StubIndex.getInstance().processAllKeys(index.key, processor, scope, filter)
        }
    }

    override fun processElementsWithName(
        name: String,
        processor: Processor<in NavigationItem>,
        parameters: FindSymbolParameters,
    ) {
        for (index in indices) {
            val elements = index.getScoped(name, parameters.project, parameters.searchScope)
            for (element in elements) {
                if (element is NavigationItem && !processor.process(element)) return
            }
        }
    }
}
