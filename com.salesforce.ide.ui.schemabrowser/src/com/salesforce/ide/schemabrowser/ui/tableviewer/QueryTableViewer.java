/*******************************************************************************
 * Copyright (c) 2014 Salesforce.com, inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Salesforce.com, inc. - initial API and implementation
 ******************************************************************************/
package com.salesforce.ide.schemabrowser.ui.tableviewer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;

import com.salesforce.ide.core.factories.ConnectionFactory;
import com.salesforce.ide.core.internal.context.ContainerDelegate;
import com.salesforce.ide.core.internal.utils.SoqlEnum;
import com.salesforce.ide.core.internal.utils.Utils;
import com.salesforce.ide.core.internal.utils.XmlConstants;
import com.salesforce.ide.core.project.ForceProjectException;
import com.salesforce.ide.core.remote.Connection;
import com.salesforce.ide.core.remote.ForceConnectionException;
import com.salesforce.ide.core.remote.ForceRemoteException;
import com.sforce.soap.partner.wsc.QueryResult;
import com.sforce.ws.ConnectionException;
import com.sforce.ws.bind.XmlObject;

/**
 * Legacy class
 *
 * @author dcarroll
 */
public class QueryTableViewer {

    private String[] columnNames = new String[] { "completed", "description", "owner", "percent" };
    private IProject project = null;
    private Composite parentComposite = null;
    protected Table table;
    protected TableViewer tableViewer;
    protected DataRowList taskList = new DataRowList();
    protected ConnectionFactory connectionFactory = null;

    //   C O N S T R U C T O R S
    public QueryTableViewer() {
        super();
    }

    //   M E T H O D S
    public ConnectionFactory getConnectionFactory() throws ForceProjectException {
        if (connectionFactory == null) {
            connectionFactory = ContainerDelegate.getInstance().getFactoryLocator().getConnectionFactory();
        }
        return connectionFactory;
    }

    public IProject getProject() {
        return project;
    }

    public void setProject(IProject project) {
        this.project = project;
    }

    public Composite getParentComposite() {
        return parentComposite;
    }

    public void setParentComposite(Composite parentComposite) {
        this.parentComposite = parentComposite;
    }

    public void initialize(Composite parent) throws ForceConnectionException, ForceProjectException,
            ForceRemoteException {
        addChildControls(parent);
    }

    /**
     * Release resources
     */
    public void dispose() {
        // Tell the label provider to release its resources
        tableViewer.getLabelProvider().dispose();
    }

    /**
     * Create a new shell, add the widgets, open the shell
     *
     * @return the shell that was created
     * @throws ConnectionException
     * @throws ConnectionException
     * @throws ForceProjectException
     * @throws ForceConnectionException
     * @throws ForceRemoteException 
     */
    private void addChildControls(final Composite composite) throws ForceConnectionException, ForceProjectException,
            ForceRemoteException {
        parentComposite = composite;
        Connection connection = getConnectionFactory().getConnection(project);
        QueryResult qr = connection.query(SoqlEnum.getSchemaInitalizationQuery());
        Table table = createTable(composite, qr);

        // Create and setup the TableViewer
        createTableViewer(table);
        tableViewer.setContentProvider(new SchemaContentProvider());
        tableViewer.setLabelProvider(new CellLabelProvider());

        // The input for the table viewer is the instance of ExampleTaskList
        taskList = new DataRowList(qr);
        tableViewer.setInput(taskList);
    }

    public void loadTable(QueryResult qr) {
        table = createTable(parentComposite, qr);

        createTableViewer(table);
        tableViewer.setContentProvider(new SchemaContentProvider());
        tableViewer.setLabelProvider(new CellLabelProvider());

        // The input for the table viewer is the instance of ExampleTaskList
        taskList = new DataRowList(qr);
        if (taskList.getTasks().size() > 0) {
            tableViewer.setInput(taskList);
            parentComposite.update();
            parentComposite.redraw();
            parentComposite.layout(true);
        }
    }

    Table createTable(QueryResult qr) {
        return createTable(parentComposite, qr);
    }

    Table createTable(Composite parent, QueryResult qr) {

        if (table != null) {
            table.dispose();
            table = null;
        }
        int style = SWT.SINGLE | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.HIDE_SELECTION;
        table = new Table(parent, style);

        table.setLinesVisible(true);
        table.setHeaderVisible(true);

        ArrayList<String> _columnNames = new ArrayList<String>();

        if (qr != null && qr.getSize() > 0) {
            if (Utils.isNotEmpty(qr.getRecords())) {
                Iterator<XmlObject> it = qr.getRecords()[0].getChildren();
                int columnTally = 0;
                while (it.hasNext()) {
                    XmlObject field = it.next();
                    String fieldNameLC = field.getName().getLocalPart();
                    if (("Id".equalsIgnoreCase(fieldNameLC) && (field.getValue() == null))
                            || ("Id".equalsIgnoreCase(fieldNameLC) && _columnNames.contains(field.getName()
                                    .getLocalPart()))) {
                        // Skip this
                    } else {
                        if (XmlConstants.ELEM_TYPE.equals(field.getName().getLocalPart())) {
                            // skip this too
                        } else {
                            if (!_columnNames.contains(field.getName().getLocalPart())) {
                                // Create a new column
                                TableColumn col = new TableColumn(table, SWT.LEFT, columnTally);
                                col.setText(field.getName().getLocalPart());
                                col.setWidth(120);
                                _columnNames.add(field.getName().getLocalPart());
                                columnTally++;
                            }
                        }

                    }
                }
            } else {
                // W-519894
            }
            columnNames = _columnNames.toArray(new String[_columnNames.size()]);
        }

        ((SashForm) parent).setWeights(new int[] { 20, 80 });
        // parent.pack();
        return table;
    }

    /**
     * Create the TableViewer
     */
    private void createTableViewer(Table table) {

        tableViewer = new TableViewer(table);

        tableViewer.setUseHashlookup(true);
        table.addMouseListener(new MouseListener() {

            public void mouseDoubleClick(MouseEvent e) {
            }

            public void mouseDown(MouseEvent e) {
            }

            public void mouseUp(MouseEvent e) {

            }
        });

        tableViewer.setColumnProperties(columnNames);
        CellEditor[] editorsx = new CellEditor[columnNames.length];
        for (int i = 0; i < columnNames.length; i++) {
            editorsx[i] = new TextCellEditor(table);
        }
        tableViewer.setCellEditors(editorsx);
        tableViewer.setCellModifier(new CellModifier(this));

        if (1 == 2) {
            // Create the cell editors
            CellEditor[] editors = new CellEditor[columnNames.length];

            // Column 1 : Completed (Checkbox)
            editors[0] = new CheckboxCellEditor(table);

            // Column 2 : Description (Free text)
            TextCellEditor textEditor = new TextCellEditor(table);
            ((Text) textEditor.getControl()).setTextLimit(60);
            editors[1] = textEditor;

            // Column 3 : Owner (Combo Box)
            editors[2] = new ComboBoxCellEditor(table, taskList.getOwners(), SWT.READ_ONLY);

            // Column 4 : Percent complete (Text with digits only)
            textEditor = new TextCellEditor(table);
            ((Text) textEditor.getControl()).addVerifyListener(

            new VerifyListener() {
                public void verifyText(VerifyEvent e) {
                    // Here, we could use a RegExp such as the following
                    // if using JRE1.4 such as e.doit =
                    // e.text.matches("[\\-0-9]*");
                    e.doit = "0123456789".indexOf(e.text) >= 0;
                }
            });
            editors[3] = textEditor;

            // Assign the cell editors to the viewer
            tableViewer.setCellEditors(editors);
            // Set the cell modifier for the viewer
            tableViewer.setCellModifier(new CellModifier(this));
            // Set the default sorter for the viewer
            tableViewer.setSorter(new DataRowSorter(DataRowSorter.DESCRIPTION));
        }
    }

    /**
     * InnerClass that acts as a proxy for the ExampleTaskList providing content for the Table. It implements the
     * ITaskListViewer interface since it must register changeListeners with the ExampleTaskList
     */
    class SchemaContentProvider implements IStructuredContentProvider, IDataRowListViewer {
        public void inputChanged(Viewer v, Object oldInput, Object newInput) {
            if (newInput != null) {
                ((DataRowList) newInput).addChangeListener(this);
            }
            if (oldInput != null) {
                ((DataRowList) oldInput).removeChangeListener(this);
            }
        }

        public void dispose() {
            taskList.removeChangeListener(this);
        }

        // Return the tasks as an array of Objects
        public Object[] getElements(Object parent) {
            return taskList.getTasks().toArray();
        }

        /*
         * (non-Javadoc)
         *
         * @see ITaskListViewer#addTask(ExampleTask)
         */
        public void addTask(DataRow task) {
            tableViewer.add(task);
        }

        /*
         * (non-Javadoc)
         *
         * @see ITaskListViewer#removeTask(ExampleTask)
         */
        public void removeTask(DataRow task) {
            tableViewer.remove(task);
        }

        /*
         * (non-Javadoc)
         *
         * @see ITaskListViewer#updateTask(ExampleTask)
         */
        public void updateTask(DataRow task) {
            tableViewer.update(task, null);
        }
    }

    /**
     * Return the column names in a collection
     *
     * @return List containing column names
     */
    public java.util.List<String> getColumnNames() {
        return Arrays.asList(columnNames);
    }

    /**
     * @return currently selected item
     */
    public ISelection getSelection() {
        return tableViewer.getSelection();
    }

    /**
     * Return the ExampleTaskList
     */
    public DataRowList getTaskList() {
        return taskList;
    }

    /**
     * Return the parent composite
     */
    public Control getControl() {
        return tableViewer.getTable().getParent();
    }

    public Table getTable() {
        return table;
    }
}