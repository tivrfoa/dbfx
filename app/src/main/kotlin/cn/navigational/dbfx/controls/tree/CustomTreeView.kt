package cn.navigational.dbfx.controls.tree

import cn.navigational.dbfx.SQLClientManager
import cn.navigational.dbfx.kit.enums.Clients
import cn.navigational.dbfx.model.DbInfo
import cn.navigational.dbfx.navigator.DatabaseItem
import cn.navigational.dbfx.navigator.impl.MysqlItem
import cn.navigational.dbfx.navigator.impl.PgItem
import cn.navigational.dbfx.utils.AlertUtils
import javafx.collections.ListChangeListener
import javafx.scene.control.TreeItem
import javafx.scene.control.TreeView
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class CustomTreeView private constructor() : TreeView<String>() {

    init {
        isShowRoot = false
        styleClass.add("l-navigator")
        this.setCellFactory { NTreeCell() }
        SQLClientManager.manager.getDbInfo().forEach(this::createClientTree)
        SQLClientManager.manager.getDbInfo().addListener(ListChangeListener {
            while (it.next()) {
                //Listener add operation
                if (it.wasAdded()) {
                    it.addedSubList.forEach(this::createClientTree)
                }
                //Listener remove operation
                if (it.wasRemoved()) {
                    it.removed.forEach(this::deleteClientTree)
                }
            }
        })
    }

    /**
     *
     * Create a new client tree
     * @param it Database info
     */
    private fun createClientTree(it: DbInfo) {
        val uuid = it.uuid
        val item = when (it.client) {
            Clients.MYSQL -> MysqlItem(uuid)
            else -> PgItem(uuid)
        }
        if (this.root == null) {
            this.root = TreeItem()
        }
        this.root.children.add(item)
    }

    /**
     * Delete a client tree
     *
     * @param it Database info
     */
    private fun deleteClientTree(it: DbInfo) {
        for (child in root.children) {
            val item = child as DatabaseItem
            if (it.uuid == item.uuid) {
                item.delete()
                break
            }
        }
    }

    fun updateConnection(dbInfo: DbInfo) {
        val list = root.children.filter { (it as DatabaseItem).uuid == dbInfo.uuid }
        if (list.isEmpty()) {
            return
        }
        val item = list[0] as DatabaseItem
        item.update()
        if (!item.getConnectionStatus()) {
            return
        }
        val result = AlertUtils.showSimpleConfirmDialog("当前连接已改变,是否重新连接?")
        if (!result) {
            return
        }
        //Start reconnection
        GlobalScope.launch {
            try {
                item.startConnect()
            } catch (e: Exception) {
                AlertUtils.showExDialog("连接失败", e)
            }
        }
    }

    companion object {
        val customTreeView: CustomTreeView by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { CustomTreeView() }
    }
}