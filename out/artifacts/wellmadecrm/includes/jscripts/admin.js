/**
 * Created by wmdcprog on 5/4/2019.
 */
Ext.onReady(function(){
    Ext.QuickTips.init();
    Ext.create('Ext.container.Viewport', {
        layout: 'border',
        id: 'viewportAdmin',
        renderTo: Ext.getBody(),
        items: [
            {
                xtype: 'panel',
                region: 'center',
                layout: 'border',
                bodyPadding: '4 4 4 4',
                margin: 0,
                items: [
                    {
                        xtype: 'panel',
                        id: 'centerPanel',
                        region: 'center',
                        layout: 'fit',
                        padding: 3,
                        items: [usersGrid]
                    }
                ]
            }
        ]
    });
});

Ext.define('Users', {
    extend: 'Ext.data.Model',
    fields: [
        {name: 'username', type: 'string'},
        {name: 'email', type: 'string'},
    ]
});

var usersStore = Ext.create('Ext.data.Store', {
    model: 'Users',
    autoLoad: true,
    proxy: {
        type: 'ajax',
        url: 'getusers',
        reader: {
            type: 'json',
            rootProperty: 'users'
        },
        actionMethods: {
            create: 'post',
            read: 'post',
            update: 'post',
            destroy: 'post'
        }
    }
});

var usersGrid = Ext.create('Ext.grid.Panel', {
    title: 'Users',
    store: usersStore,
    frame: true,
    listeners: {
        beforeitemdblclick: function(selModel, record, index, options) {
            //console.log(record.data);
            editUser();
        }
    },
    columns: [
        {
            text: '<b>Username</b>',
            autoSizeColumn: true,
            dataIndex: 'username',
            flex: 1
        },
        {
            text: '<b>Email</b>',
            autoSizeColumn: true,
            dataIndex: 'email',
            flex: 1
        }
    ]
});

function editUser()
{
    var editForm = Ext.create('Ext.form.Panel', {
        region: 'center',
        bodyStyle: 'padding: 5px',
        width: 450,
        height: 225,
        items: [],
        buttons: [
            {
                text: 'OK',
                handler: function() {
                    Ext.getCmp('editUser').close();
                }
            }
        ]
    });

    Ext.create('Ext.Window', {
        id: 'editUser',
        title: 'Sample customer',
        width: 450,
        height: 225,
        minWidth: 450,
        minHeight: 225,
        layout: 'fit',
        plain: true,
        modal: true,
        items: [editForm]
    }).show();
}