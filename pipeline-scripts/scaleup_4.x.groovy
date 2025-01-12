#!/usr/bin/env groovy

def contact = "nelluri@redhat.com"
def pipeline_id = env.BUILD_ID
def node_label = NODE_LABEL.toString()
def run_job = OPENSHIFT_4.X_SCALEUP.toString().toUpperCase()
def job_name = "SCALE-CI-OCP-4.X-SCALEUP"
def stage_name = "openshift_4.X_scaleup"
def property_file_name = "openshift_4.X_scaleup.properties"
def property_file_uri = OPENSHIFT_4.X_SCALEUP_PROPERTY_FILE.toString()

println("Current pipeline job id is '${pipeline_id}'")

stage (stage_name) {
	if (run_job) {
		currentBuild.result = "SUCCESS"
		node(node_label) {
			// Look for old property files.
			if (fileExists(property_file_name)) {
				println("Deleting the old ${property_file_name}")
				sh "rm ${property_file_name}"
			}
			// Download the properties file.
			sh "wget ${property_file_uri} -O ${property_file_name}"
			sh "cat ${property_file_name}"
			// Load the properties file.
			def properties = readProperties file: property_file_name
			println(properties)
			def job_parameters = []
			job_parameters.add([$class: 'LabelParameterValue', name: 'node', label: node_label ])
			// Convert properties to parameters.
			for (property in properties) {
				job_parameters.add([$class: 'StringParameterValue', name: property.key, value: property.value ])
			}
			println(job_parameters)
			try {
				// Call the new job with the parameters.
				job_id = build job: job_name, parameters: job_parameters
				println("${job_name} build ${job_id.getNumber()} completed successfully!")
			} catch (Exception e) {
				echo "${job_name} failed with the following error: "
				echo "${e.getMessage()}"
				// Notify the contact that the job failed.
				mail(
					to: contact,
					subject: "Pipeline job '${job_name}' failed",
					body: """\
						An error was encountered while running ${job_name}:\n\n
						${e.getMessage()}\n\n
						Pipeline job:  ${env.BUILD_URL}\n\n
						Parameters:  ${env.BUILD_URL}parameters/\n\n
						See the console output for more details:  ${env.BUILD_URL}consoleFull\n\n
					"""
				)
				currentBuild.result = "FAILURE"
				sh "exit 1"
			}
		}
	}
}
