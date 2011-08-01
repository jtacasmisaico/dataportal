var ds;

function renderGrid() {
    
    ds = new Ext.data.Store({
        autoLoad: false,
        storeId: 'searchResponse',
        url: 'xml/fakeSearch.xml',
        reader: new Ext.data.XmlReader({ 
            root: 'response',
            record: 'metadata',
            id: 'id',
            totalProperty: '@totalcount'
        },
        [
             'uuid',
             'schema',
             'title',
             'summary',
             {name: 'geo_wkt', mapping: 'extent'},
             {name: 'start_date', mapping: 'temporalextent > time_coverage_start', type: 'date'},
             {name: 'end_date', mapping: 'temporalextent > time_coverage_end', type: 'date'},
             'variables',
             {name: 'score', type: 'float'}
        ])
    });
    //ds.load();
    
    var colModel = new Ext.grid.ColumnModel([
        {header: "Score", width: 40, sortable: true, dataIndex: 'score'},
        {id: "title", header: "Title", width: 'auto', sortable: true, dataIndex: 'title'},
        {header: "Abstract", width: 25, sortable: false, dataIndex: 'summary'},
        {header: "Geo Extent (WKT)", width: 225, sortable: false, dataIndex: 'geo_wkt'},
        {header: "From date", width: 70, sortable: false, dataIndex: 'start_date', renderer: Ext.util.Format.dateRenderer('d/m/Y')},
        {header: "To date", width: 70, sortable: false, dataIndex: 'end_date', renderer: Ext.util.Format.dateRenderer('d/m/Y')},
        {header: "Variables", width: 150, sortable: false, dataIndex: 'variables'}
    ]);
    
    
    var grid = new Ext.grid.GridPanel({
        el: 'results',
        id: 'resultGrid',
        ds: ds,
        cm: colModel,
        autoHeight: true,
        autoExpandColumn: "title",
        autoScroll: true,
        viewConfig: {
            //forceFit: true,
            emptyText: "No data found."
        }
    });
    
    grid.render();
}