package com.kinnara.kecakplugins.numerictextfield;

import java.util.ArrayList;
import java.util.Collection;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class Activator implements BundleActivator {

    protected Collection<ServiceRegistration> registrationList;

    public void start(BundleContext context) {
        registrationList = new ArrayList<ServiceRegistration>();

        //Register plugin here
        registrationList.add(context.registerService(NumericField.class.getName(), new NumericField(), null));
        registrationList.add(context.registerService(EquationLoadBinder.class.getName(), new EquationLoadBinder(), null));
        registrationList.add(context.registerService(NumericValidator.class.getName(), new NumericValidator(), null));
        registrationList.add(context.registerService(ReadonlyCalculationLoadBinder.class.getName(), new ReadonlyCalculationLoadBinder(), null));
    }

    public void stop(BundleContext context) {
        for (ServiceRegistration registration : registrationList) {
            registration.unregister();
        }
    }
}