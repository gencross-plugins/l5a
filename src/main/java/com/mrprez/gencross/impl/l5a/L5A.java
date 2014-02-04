package com.mrprez.gencross.impl.l5a;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mrprez.gencross.Personnage;
import com.mrprez.gencross.Property;
import com.mrprez.gencross.history.HistoryUtil;
import com.mrprez.gencross.listener.custom.CustomAfterChangeValueListener;
import com.mrprez.gencross.value.DoubleValue;
import com.mrprez.gencross.value.IntValue;
import com.mrprez.gencross.value.StringValue;
import com.mrprez.gencross.value.Value;

public class L5A extends Personnage {
	
	@Override
	public void calculate() {
		super.calculate();
		if(phase.equals("clan")){
			calculateClan();
		}else if(phase.equals("famille")){
			calculateFamille();
		}else if(phase.equals("ecole")){
			calculateEcole();
		}else if(phase.equals("creation")){
			calculateCreation();
		}
	}
	
	private void calculateClan(){
		if(getProperty("Clan").getValue().toString().equals("")){
			errors.add("Vous devez choisir un clan");
		}
	}
	private void calculateFamille(){
		if(getProperty("famille").getValue().toString().equals("")){
			errors.add("Vous devez choisir une famille");
		}
	}
	private void calculateEcole(){
		if(getProperty("ecole").getValue().toString().equals("")){
			errors.add("Vous devez choisir une école");
		}
		for(Property competence : getProperty("Compétences").getSubProperties().getProperties().values()){
			if(competence.getValue().toString().equals("Choix")){
				errors.add("Vous devez choisir vos compétences libres");
			}
		}
		Set<String> competenceNames = new HashSet<String>();
		for(Property competence : getProperty("Compétences").getSubProperties().getProperties().values()){
			String competenceName = competence.getFullName();
			if(competence.getFullName().contains("Choix")){
				competenceName = competence.getValue().toString();
			}
			if(competenceNames.contains(competenceName) && !competenceName.contains("Choix")){
				errors.add("Vous ne pouvez avoir 2 fois la compétence: "+competenceName);
			}
			competenceNames.add(competenceName);
		}
	}
	
	private void calculateCreation(){
		if(HistoryUtil.sumHistoryOfSubTree(history, getProperty("Desavantages")).containsKey("PC")){
			if(HistoryUtil.sumHistoryOfSubTree(history, getProperty("Desavantages")).get("PC")<-10){
				errors.add("Vous ne pouvez avoir plus de 10PC de désavantages");
			}
		}
	}

	@Override
	public void passToNextPhase() throws Exception {
		super.passToNextPhase();
		if(phase.equals("famille")){
			passToFamillePhase();
		}else if(phase.equals("ecole")){
			passToEcolePhase();
		}else if(phase.equals("creation")){
			passToCreationPhase();
		}
	}
	

	private void passToFamillePhase() throws SecurityException, NoSuchMethodException{
		Map<String,String> familles = appendix.getSubMap("famille."+getProperty("Clan").getValue().toString());
		for(String famille : familles.values()){
			getProperty("famille").getOptions().add(new StringValue(famille));
		}
		getProperty("famille").setEditable(true);
		getProperty("Clan").setEditable(false);
		this.addAfterChangeValueListener(new CustomAfterChangeValueListener(this, "chooseFamilleListener", "famille"));
		
	}
	
	private void passToEcolePhase(){
		Map<String,String> ecoles = appendix.getSubMap("ecole.");
		for(String ecole : ecoles.values()){
			getProperty("ecole").getOptions().add(new StringValue(ecole));
		}
		getProperty("famille").setEditable(false);
		getProperty("ecole").setEditable(true);
		getProperty("Compétences").getSubProperties().setEditableRecursivly(false);
	}
	
	private void passToCreationPhase() throws Exception{
		// Ecole plus editable
		getProperty("ecole").setEditable(false);
		// Ajout de l'avantage ecole différente si besoin
		ecoleDifferente();
		// Possibilité d'éditer les attributs et l'anneau du vide
		for(Property anneau : getProperty("Attributs").getSubProperties()){
			if(anneau.getSubProperties()!=null){
				anneau.getSubProperties().setEditableRecursivly(true);
			}else{
				anneau.setEditable(true);
			}
		}
		// Récupération des compétences libres de l'école
		List<Property> choosenProperties = new ArrayList<Property>();
		Iterator<Property> it = getProperty("Compétences").iterator();
		while(it.hasNext()){
			Property competence = it.next();
			if(competence.getFullName().contains("Choix")){
				it.remove();
				Property newComp = getProperty("Compétences").getSubProperties().getDefaultProperty().clone();
				newComp.setName(competence.getValue().toString());
				choosenProperties.add(newComp);
			}
		}
		for(Property competence : choosenProperties){
			getProperty("Compétences").getSubProperties().add(competence);
		}
		// Compétences éditables et possibilité de nouvelles compétences
		getProperty("Compétences").getSubProperties().setEditableRecursivly(true);
		getProperty("Compétences").getSubProperties().setFixe(false);
		getProperty("Compétences").getSubProperties().setOptions((String[]) appendix.getSubMap("competence.").values().toArray(new String[0]), getProperty("Compétences").getSubProperties().getDefaultProperty());
		
		// Possibilité de nouvelles spécialitées et impossibilité de supprimer les anciennes
		for(Property competence : getProperty("Compétences").getSubProperties().getProperties().values()){
			competence.getSubProperties().setFixe(false);
			competence.setRemovable(false);
			for(Property specialite : competence.getSubProperties().getProperties().values()){
				specialite.setRemovable(false);
			}
		}
		
		getProperty("Avantages").getSubProperties().setFixe(false);
		getProperty("Desavantages").getSubProperties().setFixe(false);
		
	}
	
	public void chooseFamilleListener(String familleName, Value oldValue){
		recalculateBonus(getProperty(familleName),oldValue,"bonus_famille.");
	}
	
	private void recalculateBonus(Property property, Value oldValue, String prefix){
		String attString = appendix.getProperty(prefix+oldValue.toString());
		Property att = getProperty("Attributs#"+appendix.getProperty(attString+".anneau")+"#"+attString);
		if(att!=null){
			att.setValue(new IntValue(((IntValue)att.getValue()).getValue()-1));
		}
		attString = appendix.getProperty(prefix+property.getValue().toString());
		att = getProperty("Attributs#"+appendix.getProperty(attString+".anneau")+"#"+attString);
		att.setValue(new IntValue(((IntValue)att.getValue()).getValue()+1));
		changeAttribute(att, null);
	}
	
	public void chooseEcoleListener(Property property, Value oldValue){
		String ecoleName = property.getValue().toString();
		Property competences = getProperty("Compétences");
		competences.getSubProperties().getProperties().clear();
		Map<String,String> competenceList = appendix.getSubMap("competence_ecole."+ecoleName);
		for(String competence : competenceList.values()){
			if(competence.startsWith("Choix")){
				Property newComp = new Property(competence, "Choix", competences);
				String choix = competence.substring(6,competence.length()-1);
				String choixTab[] = choix.trim().split(",");
				List<Value> options = new ArrayList<Value>();
				for(int i=0;i<choixTab.length;i++){
					for(String option : appendix.getSubMap("competence."+choixTab[i]).values()){
						options.add(new StringValue(option));
					}
				}
				newComp.setOptions(options);
				competences.getSubProperties().add(newComp);
			}else{
				if(competence.contains("(")){
					String specialiteName = competence.substring(competence.indexOf("(")+1, competence.indexOf(")"));
					competence = competence.substring(0, competence.indexOf("("));
					Property newComp = competences.getSubProperties().getDefaultProperty().clone();
					newComp.setName(competence);
					newComp.getSubProperties().setFixe(true);
					newComp.setEditable(false);
					competences.getSubProperties().add(newComp);
					Property specialite = new Property(specialiteName, newComp);
					newComp.getSubProperties().add(specialite);
					
				}else{
					Property newComp = competences.getSubProperties().getDefaultProperty().clone();
					newComp.setName(competence);
					competences.getSubProperties().add(newComp);
					newComp.getSubProperties().setFixe(true);
					newComp.setEditable(false);
				}
			}
		}
		recalculateBonus(property, oldValue, "bonus_ecole.");
		((DoubleValue)getProperty("Honneur").getValue()).setValue(appendix.getProperty("honneur."+ecoleName));
	}
	
	public void changeAttribute(Property attribute, Value oldValue){
		Property anneau = (Property) attribute.getOwner();
		Property att1 = anneau.getSubProperties().get(0);
		Property att2 = anneau.getSubProperties().get(1);
		IntValue v1 = (IntValue) att1.getValue();
		IntValue v2 = (IntValue) att2.getValue();
		anneau.setValue(new IntValue(Math.min(v1.getValue(), v2.getValue())));
	}
	
	
	
	private void ecoleDifferente() throws Exception{
		String ecole = getProperty("ecole").getValue().toString();
		String clan = getProperty("Clan").getValue().toString();
		Map<String, String> ecolesClaniques = appendix.getSubMap("ecole."+clan);
		boolean ecoleDifferente = true;
		for(String ecoleClan : ecolesClaniques.values()){
			if(ecoleClan.equals(ecole)){
				ecoleDifferente = false;
			}
		}
		if(ecoleDifferente){
			Property avantage = getProperty("Avantages").getSubProperties().getDefaultProperty().clone();
			avantage.setValue(new IntValue(3));
			avantage.setName("Ecole différente");
			avantage.setEditable(false);
			this.addPropertyToMotherProperty(avantage);
		}
	}
	
	
}
